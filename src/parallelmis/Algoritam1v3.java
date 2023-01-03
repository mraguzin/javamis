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
import java.util.logging.Level;
import java.util.logging.Logger;
import parallelmis.helpers.Pomoćne;
import parallelmis.helpers.SharedDouble;

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
    private final double[] vjerojatnosti; // read-only
    private final CyclicBarrier b1, b2, b3;
    private final int id;
    private final int brojDretvi;
    private final AtomicBoolean gotovo;
    private final AtomicInteger brojač;
    
    public Algoritam1v3(int brojDretvi, int id, int nVrhova,
            LinkedHashSet<Integer> Vp,
            ConcurrentSkipListSet<Integer> V,
            ConcurrentSkipListSet<Integer> X,
            ArrayList<LinkedHashSet<Integer>> lista, double[] vjerojatnosti,
            LinkedHashSet<Integer>[] vrhovi, AtomicBoolean gotovo, AtomicInteger brojač,
            CyclicBarrier b1, CyclicBarrier b2, CyclicBarrier b3) {
        this.brojDretvi = brojDretvi;
        this.id = id;
        this.vrhovi = vrhovi;
        this.gotovo = gotovo;
        this.brojač = brojač;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
        this.nVrhova = nVrhova;
        this.vjerojatnosti = vjerojatnosti;
        this.listaSusjednosti = lista;
        this.V = V;
        this.Vp = Vp;
        this.X = X;
    }

    @Override
    public void run() {
        boolean dretvaGotova = false;
        
        while (!gotovo.getPlain()) {
        // prva faza radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v
        
        for (int i = 0; i < nIteracija; ++i) {
            for (int v : Vp) {
                if (vjerojatnosti[v] < Double.POSITIVE_INFINITY) {
                    double odabir = ThreadLocalRandom.current().nextDouble();
                    if (vjerojatnosti[v] < odabir)
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
        
        for (int i : Vp) { // TODO: optimizacija --- koristi finije zasebne r/w lokote za V umjesto korištenja ConcurrentSet
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
        
        if (!dretvaGotova && Vp.isEmpty()) {
            dretvaGotova = true;
            brojač.decrementAndGet();
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
