package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mraguzin
 * Ova varijanta ne računa zasebno koje dijelove skupa X koja
 * dretva mora ukloniti iz V i ne koristi barijere, nego Phasere.
 */
public class Algoritam1v4 extends Algoritam1 {
    private final List<LinkedHashSet<Integer>> Vpart2 = particionirajVrhove2(brojDretvi);
    private final LinkedHashSet<Integer> Xstar = new LinkedHashSet<>();
    private final ArrayList<LinkedHashSet<Integer>> kopijaListe = new ArrayList<>(n);
    private final Phaser phaser1, phaser2;

    public Algoritam1v4(Graf graf) {
        this(graf, dajDefaultBrojDretvi(graf));
    }
    
    public Algoritam1v4(Graf graf, int brojDretvi) {
        super(graf, brojDretvi);
        this.I = new ConcurrentLinkedQueue<>();
        
        for (int i = 0; i < n; ++i) {
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
    
    private void postavi() {
        
    }
    
    @Override
    protected void b2kraj() {
        Xstar.clear();
        
        for (int v : X) {
            I.add(v);
            Xstar.add(v);
            Xstar.addAll(kopijaListe.get(v));
            }
    }
    
    protected class Algoritam1impl implements Runnable {
        private int id;
        
        public Algoritam1impl(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            var Vp = Vpart2.get(id);
            int test = 1;
        boolean dretvaGotova = false;
        
        do {
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
        
        phaser1.arriveAndAwaitAdvance();
        
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
        
        phaser2.arriveAndAwaitAdvance();
        Vp.removeAll(Xstar);
        X.clear(); // Ovo nije atomično, ali to je ok (u najgorem slučaju može
        //doći do višestrukog procesiranja vrhova u X, što je beskorisno,
        // ali i dalje korektno). Treba izmjeriti vremena na jako velikim i
        //gustim grafovima da vidimo kako ovo zaista utječe na perf.
        for (int v : Vp) {
            if (Xstar.contains(v))
                V.remove(v);
        }
        
        if (Vp.isEmpty()) {
            if (!dretvaGotova) {
                test = brojač.decrementAndGet();
                dretvaGotova = true;
            }
            else
                test = brojač.get();
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