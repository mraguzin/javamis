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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
public class Algoritam1v2 extends Algoritam1 {    
    // bridove koje vidi su svi bridovi incidentni s gornjim vrhovima; ovo bi se moglo bolje raspodijeliti...
    private final List<LinkedHashSet<Integer>> Vpart2 = particionirajVrhove2(brojDretvi);
    private final ReadWriteLock lokot = new ReentrantReadWriteLock();
    private final Lock lokotR = lokot.readLock();
    private final Lock lokotW = lokot.writeLock();

    public Algoritam1v2(Graf graf) {
        super(graf);
        this.V = new LinkedHashSet<>(Arrays.asList(graf.dajVrhove()));
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
        var mojiVrhovi = vrhovi.get(id);
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

    @Override
    protected void b2kraj() {
        var Xstar = new TreeSet<Integer>(X);
            for (int v : X) {
                Xstar.addAll(listaSusjednosti.get(v));
                I.add(v);
            }
                
            for (int i = 0; i < brojDretvi; ++i) {
                vrhovi.set(i, new TreeSet<>(Vpart2.get(i)));
                vrhovi.get(i).retainAll(Xstar); // Vp ∩ X*
            }
    }
    
    @Override
    protected void b3kraj() {
        X.clear();
            
        if (V.isEmpty())
            gotovo.set(true);
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
