package parallelmis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mraguzin
 * Ova varijanta koristi privremene sortirane nemutabilne kopije liste
 * susjednosti. Također, implementirana je neka vrsta "work-stealing"
 * pristupa kada neka od dretvi više nema posla
 * (prošle varijante tada jednostavno besposleno prolaze kroz barijere).
 */
public class Algoritam1v3 extends Algoritam1 {
    private final List<LinkedHashSet<Integer>> particijaVrhova2 = particionirajVrhove2(brojDretvi);
    private final AtomicLong gotoveDretve = new AtomicLong();
    private final ArrayList<LinkedHashSet<Integer>> kopijaListeSusjednosti = new ArrayList<>(brojVrhova);
    private final LinkedHashSet<Integer>[] vrhovi2 = new LinkedHashSet[brojDretvi];

    public Algoritam1v3(Graf graf) {
        this(graf, dajDefaultBrojDretvi(graf));
    }
    
    public Algoritam1v3(Graf graf, int brojDretvi) {
        super(graf, brojDretvi);
        for (int i = 0; i < brojVrhova; ++i) {
            kopijaListeSusjednosti.add(new LinkedHashSet<>(listaSusjednosti.get(i)));
        }
        
        listaSusjednosti = kopijaListeSusjednosti;
    }
    
    protected class Algoritam1impl implements Runnable {
        private final int id;
        
        public Algoritam1impl(int id) {
            this.id = id;
        }
        
        @Override
        public void run() {
            var vrhoviParticije = particijaVrhova2.get(id);
            
            while (!gotovo.getPlain()) {
        // prva faza radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v
        
        for (int i = 0; i < brojIteracija; ++i) {
            for (int v : vrhoviParticije) {
                double p = 1.0 / (2.0 * listaSusjednosti.get(v).size());
                if (p < Double.POSITIVE_INFINITY) {
                    double odabir = ThreadLocalRandom.current().nextDouble();
                    if (p < odabir)
                        nezavisniVrhovi.add(v);
                    }
                else
                    nezavisniVrhovi.add(v);
                }
            }
        
        try {
            barrier1.await();
            // druga faza radi uklanjanje vrhova manjeg stupnja za bridove s vrhovima u X
        } catch (InterruptedException | BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int i : vrhoviParticije) {
            for (int j : listaSusjednosti.get(i)) {
                if (sviVrhovi.contains(j) && nezavisniVrhovi.contains(i) && nezavisniVrhovi.contains(j)) {
                    int kojiJeVeći = Integer.compare(listaSusjednosti.get(i).size(), listaSusjednosti.get(j).size());
                    if(kojiJeVeći < 0) nezavisniVrhovi.remove(i);
                    else if (kojiJeVeći > 0) nezavisniVrhovi.remove(j);
                    else nezavisniVrhovi.remove(Math.min(i, j));
                }
            }
        }
        
        try {
            barrier2.await(); // ova će barijera na kraju izvršiti računanje unije u I na jednoj (zadnjoj) dretvi
        } catch (InterruptedException | BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje X iz V
        var mojiVrhovi = vrhovi2[id];
        sviVrhovi.removeAll(mojiVrhovi);
        vrhoviParticije.removeAll(mojiVrhovi);
        
        if (vrhoviParticije.isEmpty()) {
            gotoveDretve.accumulateAndGet(id, (long x, long y) -> {
                long res = x;
                res |= 1L << y;
                return res;
            });
        }
        
            try {
                barrier3.await();
            } catch (InterruptedException | BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
        }
        
    }
    
    @Override
    protected void b2kraj() {
        var nezavisniVrhoviZvijezda = new LinkedHashSet<Integer>(nezavisniVrhovi);
            for (int v : nezavisniVrhovi) {
                maxNezavisanGraf.add(v);
                nezavisniVrhoviZvijezda.addAll(kopijaListeSusjednosti.get(v));
            }
                
            for (int i = 0; i < brojDretvi; ++i) {
                vrhovi2[i] = new LinkedHashSet<>(particijaVrhova2.get(i));
                vrhovi2[i].retainAll(nezavisniVrhoviZvijezda); // Vp ∩ X*
            }
    }
    
    @Override
    protected void b3kraj() {
        nezavisniVrhovi.clear();
            
            if (sviVrhovi.isEmpty())
                gotovo.set(true);
            else {
                long tmp = gotoveDretve.getPlain();
                int gotova;
                int aktivna;
                long rez = tmp;
                int limit = 0;
                
                    while (tmp != 0) {
                        gotova = Long.numberOfTrailingZeros(tmp);
                        if (gotova == 0)
                            aktivna = Long.numberOfTrailingZeros(~tmp);
                        else
                            aktivna = gotova - 1;
                        
                        if (aktivna >= brojDretvi || gotova >= brojDretvi)
                            break;
                        
                        int vrhova = particijaVrhova2.get(aktivna).size();
                        if (vrhova >= 2) {
                            limit = Math.max(gotova, limit);
                            var zaUkloniti = new LinkedHashSet<Integer>();
                            var iterator = particijaVrhova2.get(aktivna).iterator();
                            for (int i = 0; i < vrhova/2; ++i) {
                                int element = iterator.next();
                                particijaVrhova2.get(gotova).add(element);
                                //particijaVrhova2.get(aktivna).remove(element);
                                zaUkloniti.add(element);
                            }
                            
                            particijaVrhova2.get(aktivna).removeAll(zaUkloniti);
                            rez &= ~(1L << gotova);
                        }
                        
                        tmp &= ~(1L << gotova);
                    }
                    
                    gotoveDretve.setPlain(rez);
                }
    }
    
    @Override
    protected Collection<Integer> impl() {
        List<Thread> dretve = new ArrayList<>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i) {
            Thread dretva = new Thread(new Algoritam1impl(i));
            dretve.add(dretva);
            dretva.start();
        }
        
        for (Thread dretva : dretve)
            try {
                dretva.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return maxNezavisanGraf;
    }
    
}
