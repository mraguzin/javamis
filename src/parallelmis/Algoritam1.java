package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mraguzin
 */
public abstract class Algoritam1 {
    protected final Graf graf;
    protected final int n; // |V|
    protected final int brojDretvi;
    protected List<TreeSet<Integer>> Vpart;
    protected Set<Integer> V;
    protected Collection<Integer> I;
    protected Set<Integer> X;
    protected List<? extends Collection<Integer>> listaSusjednosti; // read-only
    protected final AtomicInteger brojač; // brojač particija; 0 znači da smo gotovi
    protected final AtomicBoolean gotovo = new AtomicBoolean(false);
    protected final CyclicBarrier b1, b2, b3;
    protected List<TreeSet<Integer>> vrhovi;
    
    protected final int nIteracija = 1;
    
    public Algoritam1(Graf graf, int brojDretvi) {
        this.graf = graf;
        this.n = graf.dajBrojVrhova();
        this.brojDretvi = brojDretvi;
        this.Vpart = particionirajVrhove(brojDretvi);
        this.V = new ConcurrentSkipListSet<>(Arrays.asList(graf.dajVrhove()));
        this.I = new ArrayList<>();
        this.X = new ConcurrentSkipListSet<>();
        this.listaSusjednosti = graf.dajListu();
        this.brojač = new AtomicInteger(brojDretvi);
        this.vrhovi = new ArrayList<>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i)
            vrhovi.add(new TreeSet<>());
        
        this.b1 = new CyclicBarrier(brojDretvi);
        this.b2 = new CyclicBarrier(brojDretvi, this::b2kraj);
        this.b3 = new CyclicBarrier(brojDretvi, this::b3kraj);
    }
    
    public Algoritam1(Graf graf) {
        this(graf,
            Runtime.getRuntime().availableProcessors() >= graf.dajBrojVrhova() ?
            graf.dajBrojVrhova() : Runtime.getRuntime().availableProcessors());
    }
    
    protected class Algoritam1impl implements Runnable {
        private final int id;
        private final double[] vjerojatnosti;
        
        protected Algoritam1impl(int id, double[] vjerojatnosti) {
            this.id = id;
            this.vjerojatnosti = vjerojatnosti;
        }

        @Override
        public void run() {
            boolean dretvaGotova = false;
            var Vp = Vpart.get(id);
        
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
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // treća i zadnja faza je uklanjanje X iz V
        var mojiVrhovi = vrhovi.get(id);
        V.removeAll(mojiVrhovi);
        Vp.removeAll(mojiVrhovi);
        
        if (!dretvaGotova && Vp.isEmpty()) {
            dretvaGotova = true;
            brojač.decrementAndGet();
        }
        
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
    
    protected void b2kraj() {
        var Xstar = new TreeSet<Integer>(X);
        for (int v : X)
            Xstar.addAll(listaSusjednosti.get(v));
        for (int i = 0; i < brojDretvi; ++i) {
            vrhovi.set(i, new TreeSet<>(Vpart.get(i)));
            vrhovi.get(i).retainAll(Xstar); // Vp ∩ X*
            }
            
        I.addAll(X);
        }
    
    protected void b3kraj() {
        X.clear();
        
        if (brojač.getPlain() == 0)
            gotovo.set(true);
    }
    
    protected ArrayList<TreeSet<Integer>> particionirajVrhove(int k) {
        var skupovi = new ArrayList<TreeSet<Integer>>(k);
        for (int i = 0; i < k; ++i)
            skupovi.add(new TreeSet<>());
        int m = n / k;
        for (int i = 0; i < n; ++i) {
            int j = i / m;
            if (j == k)
                k--;
            //if (skupovi.get(j) == null)
                //skupovi.set(j, new TreeSet<>());
            skupovi.get(j).add(i);
        }
        
        return skupovi;
    }
        
    // sigh...
    protected List<LinkedHashSet<Integer>> particionirajVrhove2(int k) {
        var skupovi = new ArrayList<LinkedHashSet<Integer>>(k);
        for (int i = 0; i < k; ++i)
            skupovi.add(new LinkedHashSet<>());
        int m = n / k;
        for (int i = 0; i < n; ++i) {
            int j = i / m;
            if (j == k)
                k--;
            //if (skupovi.get(j) == null)
              //  skupovi.set(j, new LinkedHashSet<>());
            skupovi.get(j).add(i);
        }
        
        return skupovi;
    }
    
    public FutureTask<Collection<Integer>> dajZadatak() {
        var c = (Callable<Collection<Integer>>) this::impl;
        return new FutureTask<>(c);
    }

    protected Collection<Integer> impl() {
        final double[] vjerojatnosti = new double[n];
        
        for (int i = 0; i < n; ++i) {
            vjerojatnosti[i] = 1.0 / (2.0 * listaSusjednosti.get(i).size()); // tu može doći ∞ i to je ok
        }
        
        List<Thread> dretve = new ArrayList<>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i) {
            Thread d = new Thread(new Algoritam1impl(i, vjerojatnosti));
            dretve.add(d);
            d.start();
        }
        
        for (Thread d : dretve)
            try {
                d.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return I;
    }
}
