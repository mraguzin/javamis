package parallelmis;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ova varijanta paralalenog MIS algoritma koristi finije lokote za zaštitu skupa
 * V umjesto da za taj skup koristi neku Concurrent kolekciju.
 * 
 * @author mraguzin
 */
public class Algoritam1v2 extends Algoritam1 {    
    // bridove koje vidi su svi bridovi incidentni s gornjim vrhovima; ovo bi se moglo bolje raspodijeliti...
    private final List<LinkedHashSet<Integer>> particijaVrhova2 = particionirajVrhove2(brojDretvi);
    private final ReadWriteLock lokot = new ReentrantReadWriteLock();
    private final Lock lokotRead = lokot.readLock();
    private final Lock lokotWrite = lokot.writeLock();

    public Algoritam1v2(Graf graf, int brojDretvi) {
        super(graf, brojDretvi);
        this.sviVrhovi = new LinkedHashSet<>(Arrays.asList(graf.dajVrhove()));
    }
    
    public Algoritam1v2(Graf graf) {
        this(graf, dajDefaultBrojDretvi(graf));
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
            Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        lokotRead.lock();
            try {
                for (int i : vrhoviParticije) {
                    for (int j : listaSusjednosti.get(i)) {
                        if (sviVrhovi.contains(j) && nezavisniVrhovi.contains(i) && nezavisniVrhovi.contains(j)) {
                            int kojiJeVeći = Integer.compare(listaSusjednosti.get(i).size(), listaSusjednosti.get(j).size());
                            if(kojiJeVeći < 0) nezavisniVrhovi.remove(i);
                            else if (kojiJeVeći > 0) nezavisniVrhovi.remove(j);
                            else nezavisniVrhovi.remove(Math.min(i, j));
                        }
                    }
                }   } finally {
                lokotRead.unlock();
            }
        
        try {
            barrier2.await(); // ova će barijera na kraju izvršiti računanje unije u I na jednoj (zadnjoj) dretvi
        } catch (InterruptedException | BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
        }

        // treća i zadnja faza je uklanjanje svih vrhova nezavisnog grafa i njihovih susjeda iz sviVrhovi
        var mojiVrhovi = vrhovi.get(id);
        lokotWrite.lock();
            try {
                sviVrhovi.removeAll(mojiVrhovi);
            } finally {
                lokotWrite.unlock();
            }
            
        vrhoviParticije.removeAll(mojiVrhovi);
        
            try {
                barrier3.await();
            } catch (InterruptedException | BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1v2.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    }

    @Override
    protected void b2kraj() {
        var nezavisniVrhoviZvijezda = new LinkedHashSet<>(nezavisniVrhovi);
            for (int v : nezavisniVrhovi) {
                nezavisniVrhoviZvijezda.addAll(listaSusjednosti.get(v));
                maxNezavisanGraf.add(v);
            }
                
            for (int i = 0; i < brojDretvi; ++i) {
                vrhovi.set(i, nezavisniVrhoviZvijezda);
            }
    }
    
    @Override
    protected void b3kraj() {
        nezavisniVrhovi.clear();
            
        if (sviVrhovi.isEmpty())
            gotovo.set(true);
    }
    
    //TODO: ovome se treba dodijeliti instanca unutarnje impl.klase t.d. ovo može biti dijeljeno u svim varijantama iz apstraktne klase
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
