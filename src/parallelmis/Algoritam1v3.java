package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
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
public class Algoritam1v3 implements Runnable {
    private int nVrhova; // vrhovi koje ova dretva vidi
    private final LinkedHashSet<Integer>[] vrhovi; // sadrži particiju konačnog X na dretve, za 3. fazu;
    //particiju određuje zadnja dretva na 2. barijeri
    
    // bridove koje vidi su svi bridovi incidentni s gornjim vrhovima; ovo bi se moglo bolje raspodijeliti...
    private final int nIteracija = 1; // koliko puta pokušati izabrati vrh za X
    private final ConcurrentSkipListSet<Integer> V;
    private final LinkedHashSet<Integer> Vp;
    private final ConcurrentSkipListSet<Integer> X;
    private final ArrayList<LinkedHashSet<Integer>> listaSusjednosti; // read-only
    private final CyclicBarrier b1, b2, b3;
    private final int id;
    private final int brojDretvi;
    private final AtomicBoolean gotovo;
    private final AtomicLong gotoveDretve;
    
    public Algoritam1v3(int brojDretvi, int id, int nVrhova,
            LinkedHashSet<Integer> Vp,
            ConcurrentSkipListSet<Integer> V,
            ConcurrentSkipListSet<Integer> X,
            ArrayList<LinkedHashSet<Integer>> lista,
            LinkedHashSet<Integer>[] vrhovi, AtomicBoolean gotovo,
            AtomicLong gotoveDretve,
            CyclicBarrier b1, CyclicBarrier b2, CyclicBarrier b3) {
        this.brojDretvi = brojDretvi;
        this.id = id;
        this.vrhovi = vrhovi;
        this.gotovo = gotovo;
        this.gotoveDretve = gotoveDretve;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
        this.nVrhova = nVrhova;
        this.listaSusjednosti = lista;
        this.V = V;
        this.Vp = Vp;
        this.X = X;
    }

    @Override
    public void run() {
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
        var mojiVrhovi = vrhovi[id];
        V.removeAll(mojiVrhovi);
        Vp.removeAll(mojiVrhovi);
        
        //if ((gotoveDretve.get() & (1 << id)) != 0 && Vp.isEmpty()) { // TODO: ovdje bi trebalo biti ok koristiti getPlain
        if (Vp.isEmpty()) { // TODO: ovdje bi trebalo biti ok koristiti getPlain
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
