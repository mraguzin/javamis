package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
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
public class Algoritam1v4 implements Runnable {
    private final int nIteracija = 1; // koliko puta pokušati izabrati vrh za X
    private final ConcurrentSkipListSet<Integer> V;
    private final LinkedHashSet<Integer> Vp;
    private final ConcurrentSkipListSet<Integer> X;
    private final LinkedHashSet<Integer> Xstar;
    private final ConcurrentLinkedQueue<Integer> I;
    private final ArrayList<LinkedHashSet<Integer>> listaSusjednosti; // read-only
    private final Phaser phaser1, phaser2;
    private final int id;
    private final int brojDretvi;
    private final AtomicInteger brojač;
    private final AtomicBoolean gotovo;
    private final AtomicLong gotoveDretve;
    
    public Algoritam1v4(int brojDretvi, int id, int nVrhova,
            LinkedHashSet<Integer> Vp,
            ConcurrentSkipListSet<Integer> V,
            ConcurrentLinkedQueue<Integer> I,
            ConcurrentSkipListSet<Integer> X,
            LinkedHashSet<Integer> Xstar,
            ArrayList<LinkedHashSet<Integer>> lista,
            AtomicBoolean gotovo,
            AtomicLong gotoveDretve,
            AtomicInteger brojač,
            Phaser phaser1, Phaser phaser2) {
        this.brojDretvi = brojDretvi;
        this.id = id;
        this.gotovo = gotovo;
        this.gotoveDretve = gotoveDretve;
        this.brojač = brojač;
        this.phaser1 = phaser1;
        this.phaser2 = phaser2;
        this.listaSusjednosti = lista;
        this.V = V;
        this.Vp = Vp;
        this.X = X;
        this.Xstar = Xstar;
        this.I = I;
    }

    @Override
    public void run() {
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
        //V.removeAll(Xstar);
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