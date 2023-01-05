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
 * Ova varijanta 3. implementacije ne računa zasebno koje dijelove skupa X koja
 * dretva mora ukloniti iz V i ne koristi barijere... ne radi dobro, zanemariti!
 */
public class Algoritam1v4 implements Runnable {
    private final int nIteracija = 1; // koliko puta pokušati izabrati vrh za X
    private final ConcurrentSkipListSet<Integer> V;
    private final LinkedHashSet<Integer> Vp;
    private final ConcurrentSkipListSet<Integer> X;
    private final ConcurrentLinkedQueue<Integer> I;
    private final ArrayList<LinkedHashSet<Integer>> listaSusjednosti; // read-only
    private final CyclicBarrier b1, b2, b3;
    private final int id;
    private final int brojDretvi;
    private final AtomicBoolean gotovo;
    private final AtomicLong gotoveDretve;
    
    public Algoritam1v4(int brojDretvi, int id, int nVrhova,
            LinkedHashSet<Integer> Vp,
            ConcurrentSkipListSet<Integer> V,
            ConcurrentLinkedQueue<Integer> I,
            ConcurrentSkipListSet<Integer> X,
            ArrayList<LinkedHashSet<Integer>> lista,
            AtomicBoolean gotovo,
            AtomicLong gotoveDretve,
            CyclicBarrier b1, CyclicBarrier b2, CyclicBarrier b3) {
        this.brojDretvi = brojDretvi;
        this.id = id;
        this.gotovo = gotovo;
        this.gotoveDretve = gotoveDretve;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
        this.listaSusjednosti = lista;
        this.V = V;
        this.Vp = Vp;
        this.X = X;
        this.I = I;
    }

    @Override
    public void run() {
        while (!V.isEmpty()) {
            System.out.println(X.toString());
            System.out.println("id:"+id+",Vp="+Vp.toString());

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
            Logger.getLogger(Algoritam1v4.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v4.class.getName()).log(Level.SEVERE, null, ex);
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
            b2.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(Algoritam1v4.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v4.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje X iz V
        var it = Vp.iterator();
        while (it.hasNext()) {
            int v = it.next();
            if (X.contains(v)) {
                I.add(v);
                Vp.remove(v);
                V.remove(v);
                Vp.removeAll(listaSusjednosti.get(v));
                V.removeAll(listaSusjednosti.get(v));
            }
        }
        
        X.clear(); // ovo nije atomično
        }
    }
    
}