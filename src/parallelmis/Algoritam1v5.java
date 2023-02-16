package parallelmis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ova je malo optimizirana varijanta v3: ne računa particiju X-a zasebno na
 * barijeri već račun V\X paralelizira na sve dretve.
 * 
 * @author mraguzin
 * @deprecated
 */
public class Algoritam1v5 implements Runnable {
    private final ConcurrentSkipListSet<Integer> sviVrhovi;
    private final LinkedHashSet<Integer> particijaVrhova;
    private final ConcurrentSkipListSet<Integer> nezavisniVrhovi;
    private final LinkedHashSet<Integer> nezavisniVrhoviZvijezda;
    private final ArrayList<LinkedHashSet<Integer>> listaSusjednosti; // read-only
    private final CyclicBarrier barrier1, barrier2, barrier3;
    private final int id;
    private final AtomicBoolean gotovo;
    private final AtomicLong gotoveDretve;
    
    public Algoritam1v5(int id,
                        LinkedHashSet<Integer> particijaVrhova,
                        ConcurrentSkipListSet<Integer> sviVrhovi,
                        ConcurrentSkipListSet<Integer> nezavisniVrhovi,
                        ArrayList<LinkedHashSet<Integer>> lista,
                        LinkedHashSet<Integer> nezavisniVrhoviZvijezda, AtomicBoolean gotovo,
                        AtomicLong gotoveDretve,
                        CyclicBarrier barrier1, CyclicBarrier barrier2, CyclicBarrier barrier3) {
        this.id = id;
        this.gotovo = gotovo;
        this.gotoveDretve = gotoveDretve;
        this.barrier1 = barrier1;
        this.barrier2 = barrier2;
        this.barrier3 = barrier3;
        this.listaSusjednosti = lista;
        this.sviVrhovi = sviVrhovi;
        this.particijaVrhova = particijaVrhova;
        this.nezavisniVrhovi = nezavisniVrhovi;
        this.nezavisniVrhoviZvijezda = nezavisniVrhoviZvijezda;
    }

    @Override
    public void run() {
        while (!gotovo.getPlain()) {
        // prva faza radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v

            // koliko puta pokušati izabrati vrh za X
            int brojIteracija = 1;
            for (int i = 0; i < brojIteracija; ++i) {
            for (int v : particijaVrhova) {
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
            Logger.getLogger(Algoritam1v5.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int i : particijaVrhova) {
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
            Logger.getLogger(Algoritam1v5.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje X iz V
        particijaVrhova.removeAll(nezavisniVrhoviZvijezda);
        
        if (particijaVrhova.isEmpty()) {
            gotoveDretve.accumulateAndGet(id, (long x, long y) -> {
                long res = x;
                res |= 1L << y;
                return res;
            });
        }
        
            try {
                barrier3.await();
            } catch (InterruptedException | BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1v5.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    
}
