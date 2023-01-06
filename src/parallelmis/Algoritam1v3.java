package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    //private final LinkedHashSet<Integer>[] vrhovi;
    private final List<LinkedHashSet<Integer>> Vpart2 = particionirajVrhove2(brojDretvi);
    //private final ArrayList<LinkedHashSet<Integer>> listaSusjednosti; // read-only
    private final AtomicLong gotoveDretve = new AtomicLong();
    private final ArrayList<LinkedHashSet<Integer>> kopijaListe = new ArrayList<>(n); // kopija liste susjednosti
    private final LinkedHashSet<Integer>[] vrhovi2 = new LinkedHashSet[brojDretvi];

    public Algoritam1v3(Graf graf) {
        super(graf);
        for (int i = 0; i < n; ++i) {
            kopijaListe.add(new LinkedHashSet<>(listaSusjednosti.get(i)));
        }
        
        listaSusjednosti = kopijaListe;
    }
    
    protected class Algoritam1impl implements Runnable {
        private int id;
        
        public Algoritam1impl(int id) {
            this.id = id;
        }
        
        @Override
        public void run() {
            var Vp = Vpart2.get(id);
            
            while (!gotovo.getPlain()) {
        // prva faza radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v
        
        for (int i = 0; i < nIteracija; ++i) {
            for (int v : Vp) {
                double p = 1.0 / (2.0 * listaSusjednosti.get(v).size());
                if (p < Double.POSITIVE_INFINITY) {
                    double odabir = ThreadLocalRandom.current().nextDouble();
                    if (p < odabir)
                        X.add(v);
                    }
                else
                    X.add(v);
                }
            }
        
        try {
            b1.await();
            // druga faza radi uklanjanje vrhova manjeg stupnja za bridove s vrhovima u X
        } catch (InterruptedException ex) {
            Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int i : Vp) {
            for (int j : listaSusjednosti.get(i)) {
                if (V.contains(j) && X.contains(i) && X.contains(j)) {
                    if (listaSusjednosti.get(i).size() <= listaSusjednosti.get(j).size()) {
                        if (listaSusjednosti.get(i).size() < listaSusjednosti.get(j).size())
                            X.remove(i);
                        else if (i < j)
                            X.remove(i);
                        else
                            X.remove(j);
                    }
                    else
                        X.remove(j);
                }
            }
        }
        
        try {
            b2.await(); // ova će barijera na kraju izvršiti računanje unije u I na jednoj (zadnjoj) dretvi
        } catch (InterruptedException ex) {
            Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje X iz V
        var mojiVrhovi = vrhovi2[id];
        V.removeAll(mojiVrhovi);
        Vp.removeAll(mojiVrhovi);
        
        if (Vp.isEmpty()) {
            gotoveDretve.accumulateAndGet(id, (long x, long y) -> {
                long res = x;
                res |= 1l << y;
                return res;
            });
        }
        
            try {
                b3.await();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1v3.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
        }
        
    }
    
    @Override
    protected void b2kraj() {
        var Xstar = new LinkedHashSet<Integer>(X);
            for (int v : X) {
                I.add(v);
                Xstar.addAll(kopijaListe.get(v));
            }
                
            for (int i = 0; i < brojDretvi; ++i) {
                vrhovi2[i] = new LinkedHashSet<>(Vpart2.get(i));
                vrhovi2[i].retainAll(Xstar); // Vp ∩ X*
            }
    }
    
    @Override
    protected void b3kraj() {
        X.clear();
            
            if (V.isEmpty())
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
                        
                        int vrhova = Vpart2.get(aktivna).size();
                        if (vrhova >= 2) {
                            limit = gotova > limit ? gotova : limit;
                            var it = Vpart2.get(aktivna).iterator();
                            for (int i = 0; i < vrhova/2; ++i) {
                                int el = it.next();
                                Vpart2.get(gotova).add(el);
                                Vpart2.get(aktivna).remove(el);
                            }
                            
                            rez &= ~(1l << gotova);
                        }
                        
                        tmp &= ~(1l << gotova);
                    }
                    
                    gotoveDretve.setPlain(rez);
                }
    }
    
    @Override
    protected Collection<Integer> impl() {
        List<Thread> dretve = new ArrayList<>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i) {
            Thread d = new Thread(new Algoritam1impl(i));
            dretve.add(d);
            d.start();
        }
        
        for (Thread d : dretve)
            try {
                d.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return I;
    }
    
}
