package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;
import parallelmis.helpers.Pomoćne;

/**
 *
 * @author mraguzin
 */
public class Algoritam1Dio1 implements Runnable {
    private final int prviVrh; // indeksi
    private final int nVrhova; // raspon vrhova koje ova dretva vidi: [prviVrh,prviVrh+1,...,prviVrh+n-1]
    private final ConcurrentLinkedQueue<Integer> V;
    private final ConcurrentSkipListSet<Integer> X;
    //private final ArrayList<ArrayList<Integer>> listaSusjednosti; // read-only
    private final ArrayList<Double> vjerojatnosti; // read-only
    
    public Algoritam1Dio1(int prviVrh, int nVrhova, ConcurrentLinkedQueue<Integer> V, ConcurrentSkipListSet<Integer> X, ArrayList<Double> vjerojatnosti) {
        this.prviVrh = prviVrh;
        this.nVrhova = nVrhova;
        this.vjerojatnosti = vjerojatnosti;
        this.V = V;
        this.X = X;
    }

    @Override
    public void run() {
        // prvi dio radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v
        
        double odabir = ThreadLocalRandom.current().nextDouble(vjerojatnosti.get(vjerojatnosti.size()-1));
        var podskup = vjerojatnosti.subList(prviVrh, prviVrh + nVrhova);
        var tmp = new Double[nVrhova];
        //int lok = Collections.binarySearch(podskup, odabir); // neće ići, treba nam baš zadnja pojava jer može se pojaviti
        // cijeli podniz vrhova stupnja 0, a oni su već morali biti odabrani u glavnoj dretvi
        int lok = Pomoćne.zadnji(vjerojatnosti.toArray(tmp), odabir, 0, nVrhova-1);
        if (lok >= 0)
            X.add(prviVrh + lok);
    }
    
}
