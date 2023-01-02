package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
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
 */
public class Algoritam1v1 implements Runnable {
    private int nVrhova; // vrhovi koje ova dretva vidi
    private final ArrayList<Integer>[] vrhovi; // sadrži particiju konačnog X na dretve, za 3. fazu;
    //particiju određuje zadnja dretva na 2. barijeri
    
    // bridove koje vidi su svi bridovi incidentni s gornjim vrhovima; ovo bi se moglo bolje raspodijeliti...
    private final int nIteracija = 1; // koliko puta pokušati izabrati vrh za X
    private final ConcurrentSkipListSet<Integer> V;
    private final ConcurrentSkipListSet<Integer> Vp;
    private final ConcurrentSkipListSet<Integer> X;
    private final ArrayList<ArrayList<Integer>> listaSusjednosti; // read-only
    private final double[] vjerojatnosti; // read-only
    private final SharedDouble maxp; // maksimalna kumulativna vjerojatnosti za trenutni V;
    // ažurira je zadnja dretva na zadnjoj barijeri u svakoj iteraciji
    private TreeMap<Double,Integer> p; // kumulativne vjerojatnosti
    private final CyclicBarrier b1, b2, b3;
    private final int id;
    private final int brojDretvi;
    private final AtomicBoolean gotovo;
    private final AtomicInteger brojač;
    
    public Algoritam1v1(int brojDretvi, int id, int nVrhova,
            ConcurrentSkipListSet<Integer> Vp,
            ConcurrentSkipListSet<Integer> V,
            ConcurrentSkipListSet<Integer> X,
            ArrayList<ArrayList<Integer>> lista, double[] vjerojatnosti, SharedDouble maxp,
            ArrayList<Integer>[] vrhovi, AtomicBoolean gotovo, AtomicInteger brojač,
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
        this.maxp = maxp;
        this.listaSusjednosti = lista;
        //this.I = I;
        this.V = V;
        this.Vp = Vp;
        this.X = X;
    }

    @Override
    public void run() {
        while (!gotovo.getPlain()) {
        // prva faza radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v
        
        double prošla = 0.0;
        p = new TreeMap<>();
        for (int v : Vp) {
            if (vjerojatnosti[v] < Double.POSITIVE_INFINITY) {
                p.put(prošla + vjerojatnosti[v], v);
                prošla += vjerojatnosti[v];
            }
            else
                X.add(v);
        }
        
        for (int i = 0; i < nIteracija; ++i) {
            double odabir = ThreadLocalRandom.current().nextDouble(maxp.get());
            // TODO: treba li ovdje svaka dretva nezavisno birati samo iz svog podskupa vrhova?
            // Onda nije potrebno birati iz globalnog raspona vjerojatnosti, nego samo iz Vp
            var par1 = p.floorEntry(odabir);
            var par2 = p.ceilingEntry(odabir);
            if (par1 != null)
                X.add(par1.getValue());
            else if (par2 != null)
                X.add(par2.getValue());
            //if (par != null)
              //  X.add(par.getValue());
        }
        
        try {
            b1.await();
            // druga faza radi uklanjanje vrhova manjeg stupnja za bridove s vrhovima u X
        } catch (InterruptedException ex) {
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje S iz V
        ArrayList<Integer> mojiVrhovi = vrhovi[id];
        V.removeAll(mojiVrhovi);
        Vp.removeAll(mojiVrhovi);
        for (int v : mojiVrhovi) {
            var susjedi = listaSusjednosti.get(v);
            V.removeAll(susjedi);
            Vp.removeAll(susjedi);
        }
        
        if (Vp.isEmpty())
            brojač.decrementAndGet();
        
            try {
                b3.await();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    
}
