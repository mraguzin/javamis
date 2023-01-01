package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import parallelmis.helpers.Pomoćne;

/**
 *
 * @author mraguzin
 */
public class Algoritam1Dio1 implements Runnable {
    private final int prviVrh; // indeksi
    private final int nVrhova; // raspon vrhova koje ova dretva vidi: [prviVrh,prviVrh+1,...,prviVrh+n-1]
    private final ArrayList<Integer>[] vrhovi; // sadrži particiju konačnog X na dretve, za 3. fazu;
    //particiju određuje zadnja dretva na 2. barijeri
    
    // bridove koje vidi su svi bridovi incidentni s gornjim vrhovima; ovo bi se moglo bolje raspodijeliti...
    private final int nIteracija = 3; // koliko puta pokušati izabrati vrh za X
    private final ConcurrentSkipListSet<Integer> V;
    private final ConcurrentSkipListSet<Integer> X;
    private final ArrayList<ArrayList<Integer>> listaSusjednosti; // read-only
    private final double[] vjerojatnosti; // read-only
    private final CyclicBarrier b1, b2, b3;
    private final int id;
    private final int brojDretvi;
    
    public Algoritam1Dio1(int brojDretvi, int id, int prviVrh, int nVrhova, ConcurrentSkipListSet<Integer> V,
            ConcurrentSkipListSet<Integer> X,
            ArrayList<ArrayList<Integer>> lista, double[] vjerojatnosti,
            ArrayList<Integer>[] vrhovi,
            CyclicBarrier b1, CyclicBarrier b2, CyclicBarrier b3) {
        this.brojDretvi = brojDretvi;
        this.id = id;
        this.vrhovi = vrhovi;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
        this.prviVrh = prviVrh;
        this.nVrhova = nVrhova;
        this.vjerojatnosti = vjerojatnosti;
        this.listaSusjednosti = lista;
        //this.I = I;
        this.V = V;
        this.X = X;
    }

    @Override
    public void run() {
        while (!V.isEmpty()) {
        // prva faza radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v
        
        for (int i = 0; i < nIteracija; ++i) {
            double odabir = ThreadLocalRandom.current().nextDouble(vjerojatnosti[vjerojatnosti.length-1]);
            int lok = Pomoćne.zadnji(vjerojatnosti, odabir, prviVrh, prviVrh+nVrhova-1);
            if (lok >= 0)
                X.add(prviVrh + lok);
        }
        
        try {
            b1.await();
            // druga faza radi uklanjanje vrhova manjeg stupnja za bridove s vrhovima u X
        } catch (InterruptedException ex) {
            Logger.getLogger(Algoritam1Dio1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1Dio1.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int i = prviVrh; i < prviVrh + nVrhova; ++i) {
            for (int j : listaSusjednosti.get(i)) {
                if (X.contains(i) && X.contains(j)) {
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
            Logger.getLogger(Algoritam1Dio1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1Dio1.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje S iz V
        ArrayList<Integer> mojiVrhovi = vrhovi[id];
        V.removeAll(mojiVrhovi);
        for (int v : mojiVrhovi) {
            var susjedi = listaSusjednosti.get(v);
            V.removeAll(susjedi);
        }
        
            try {
                b3.await();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1Dio1.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1Dio1.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    }
    
}
