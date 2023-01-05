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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import parallelmis.helpers.Pomoćne;
import parallelmis.helpers.SharedDouble;

/**
 *
 * @author mraguzin
 * Ova varijanta paralalenog MIS algoritma koristi finije lokote za zaštitu skupa
 * V umjesto da za taj skup koristi neku Concurrent kolekciju.
 */
public class Algoritam1v2 implements Runnable {
    private int nVrhova; // vrhovi koje ova dretva vidi
    private final TreeSet<Integer>[] vrhovi; // sadrži particiju konačnog X na dretve, za 3. fazu;
    //particiju određuje zadnja dretva na 2. barijeri
    
    // bridove koje vidi su svi bridovi incidentni s gornjim vrhovima; ovo bi se moglo bolje raspodijeliti...
    private final int nIteracija = 1; // koliko puta pokušati izabrati vrh za X
    private final LinkedHashSet<Integer> V;
    private final LinkedHashSet<Integer> Vp;
    private final ConcurrentSkipListSet<Integer> X;
    private final ArrayList<ArrayList<Integer>> listaSusjednosti; // read-only
    private final CyclicBarrier b1, b2, b3;
    private final int id;
    private final int brojDretvi;
    private final AtomicBoolean gotovo;
    private final Lock lokotR;
    private final Lock lokotW;
    
    public Algoritam1v2(int brojDretvi, int id, int nVrhova,
            LinkedHashSet<Integer> Vp,
            LinkedHashSet<Integer> V,
            ConcurrentSkipListSet<Integer> X,
            ArrayList<ArrayList<Integer>> lista,
            TreeSet<Integer>[] vrhovi, AtomicBoolean gotovo,
            CyclicBarrier b1, CyclicBarrier b2, CyclicBarrier b3,
            Lock lokotR, Lock lokotW) {
        this.brojDretvi = brojDretvi;
        this.id = id;
        this.vrhovi = vrhovi;
        this.gotovo = gotovo;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
        this.nVrhova = nVrhova;
        this.listaSusjednosti = lista;
        this.V = V;
        this.Vp = Vp;
        this.X = X;
        this.lokotR = lokotR;
        this.lokotW = lokotW;
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
            Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        lokotR.lock();
            try {
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
                }   } finally {
                lokotR.unlock();
            }
        
        try {
            b2.await(); // ova će barijera na kraju izvršiti računanje unije u I na jednoj (zadnjoj) dretvi
        } catch (InterruptedException ex) {
            Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje X iz V
        var mojiVrhovi = vrhovi[id];
        lokotW.lock();
            try {
                V.removeAll(mojiVrhovi);
            } finally {
                lokotW.unlock();
            }
            
        Vp.removeAll(mojiVrhovi);
        
            try {
                b3.await();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    
}
