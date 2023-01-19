package parallelmis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mraguzin
 * Ova varijanta ne računa zasebno koje dijelove skupa X koja
 * dretva mora ukloniti iz V i ne koristi barijere, nego Phasere.
 * Vremenski, ovo je *najsporija* implementacija i možemo je zanemariti (uostalom, ima i bugova!)
 */
public class Algoritam1v4 extends Algoritam1 {
    private final List<LinkedHashSet<Integer>> particijaVrhova2 = particionirajVrhove2(brojDretvi);
    private final LinkedHashSet<Integer> nezavisniVrhoviZvijezda = new LinkedHashSet<>();
    private final ArrayList<LinkedHashSet<Integer>> kopijaListe = new ArrayList<>(brojVrhova);
    private final Phaser phaser1, phaser2;

    public Algoritam1v4(Graf graf) {
        this(graf, dajDefaultBrojDretvi(graf));
    }
    
    public Algoritam1v4(Graf graf, int brojDretvi) {
        super(graf, brojDretvi);
        this.maxNezavisanGraf = new ConcurrentLinkedQueue<>();
        
        for (int i = 0; i < brojVrhova; ++i) {
            kopijaListe.add(new LinkedHashSet<>(listaSusjednosti.get(i)));
        }
        
        listaSusjednosti = kopijaListe;
        phaser1 = new Phaser(brojDretvi) {
            @Override
            protected boolean onAdvance(int phase, int registered) {
                return false;
            }
        };
        
        phaser2 = new Phaser(brojDretvi) {
            @Override
            protected boolean onAdvance(int phase, int registered) {
                b2kraj();
                return false;
            }
        };
    }
    
    @Override
    protected void b2kraj() {
        nezavisniVrhoviZvijezda.clear();
        
        for (int v : nezavisniVrhovi) {
            maxNezavisanGraf.add(v);
            nezavisniVrhoviZvijezda.add(v);
            nezavisniVrhoviZvijezda.addAll(kopijaListe.get(v));
            }
    }
    
    protected class Algoritam1impl implements Runnable {
        private final int id;
        
        public Algoritam1impl(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            var vrhoviParticije = particijaVrhova2.get(id);
            int test = 1;
        boolean dretvaGotova = false;
        
        do {
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
        
        phaser1.arriveAndAwaitAdvance();
        
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
        
        phaser2.arriveAndAwaitAdvance();
        vrhoviParticije.removeAll(nezavisniVrhoviZvijezda);
        if (id == 0)
            nezavisniVrhovi.clear(); // Ovo nije atomično, ali to je ok (u najgorem slučaju može
        //doći do višestrukog procesiranja vrhova u X, što je beskorisno,
        // ali i dalje korektno). Treba izmjeriti vremena na jako velikim i
        //gustim grafovima da vidimo kako ovo zaista utječe na perf.
        for (int v : vrhoviParticije) {
            if (nezavisniVrhoviZvijezda.contains(v))
                sviVrhovi.remove(v);
        }
        
        if (vrhoviParticije.isEmpty()) {
            if (!dretvaGotova) {
                test = brojačParticija.decrementAndGet();
                dretvaGotova = true;
            }
            else
                test = brojačParticija.get();
        }
        } while (test != 0);
        
        phaser1.forceTermination();
        phaser2.forceTermination();
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