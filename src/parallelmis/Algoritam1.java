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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
 * Glavna klasa za algoritam kojim rješavamo MIS problem; postoje razne varijante
 * konkretnih implementacija koje su isprobavane i neke su se pokazale dobrima,
 * neke baš i ne. Sve nasljeđuju ovu klasu i implementiraju svoj run() u unutarnjoj
 * implementacijskoj klasi, kako bi bilo moguće konstruirati dretve koje dijele
 * određeni kontekst, koji je sadržan pod Algoritam1 (osim nekih specifičnih
 * objekata koje može definirati svaka zasebna konkretna klasa koja nasljeđuje
 * ovu).
 */
public abstract class Algoritam1 {
    protected final Graf graf;
    protected final int n; // |V|
    protected final int brojDretvi;
    protected List<LinkedHashSet<Integer>> Vpart;
    protected Collection<Integer> V;
    protected Collection<Integer> I;
    protected final Set<Integer> X;
    protected final Set<Integer> Xstar2;
    protected List<? extends Collection<Integer>> listaSusjednosti; // read-only
    protected final AtomicInteger brojač; // brojač particija; 0 znači da smo gotovi
    protected final AtomicBoolean gotovo = new AtomicBoolean(false);
    protected final CyclicBarrier b0, b1, b2, b2dodatno, b3;
    protected List<LinkedHashSet<Integer>> vrhovi;
    
    protected final int nIteracija = 1;
    
    public Algoritam1(Graf graf, int brojDretvi) {
        this.graf = graf;
        this.n = graf.dajBrojVrhova();
        this.brojDretvi = brojDretvi;
        this.Vpart = particionirajVrhove2(brojDretvi);
        this.V = new ConcurrentSkipListSet<>(Arrays.asList(graf.dajVrhove()));
        this.I = new ConcurrentLinkedQueue<>();
        this.X = new ConcurrentSkipListSet<>();
        this.Xstar2 = new ConcurrentSkipListSet<>();
        this.listaSusjednosti = graf.dajListu();
        this.brojač = new AtomicInteger(brojDretvi);
        this.vrhovi = new ArrayList<>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i)
            vrhovi.add(new LinkedHashSet<>());
        
        this.b0 = new CyclicBarrier(brojDretvi);
        this.b1 = new CyclicBarrier(brojDretvi);
        this.b2 = new CyclicBarrier(brojDretvi, this::b2kraj);
        this.b2dodatno = new CyclicBarrier(brojDretvi, this::b2dodkraj);
        this.b3 = new CyclicBarrier(brojDretvi, this::b3kraj);
    }
    
    public Algoritam1(Graf graf) {
        this(graf, dajDefaultBrojDretvi(graf));
    }
    
    protected class Algoritam1impl implements Runnable {
        private final int id;
        private final int vrhovaPoDretvi;
        //private final ArrayList<LinkedHashSet<Integer>> kopijaListe = new ArrayList<>(n); // kopija liste susjednosti
        
        protected Algoritam1impl(int id, int vrhovaPoDretvi) {
            this.id = id;
            this.vrhovaPoDretvi = vrhovaPoDretvi;
            
            // sporije zbog kopiranja; ne isplati se za imalo veće grafove
            
//            for (int i = 0; i < n; ++i) {
//            kopijaListe.add(new LinkedHashSet<>(listaSusjednosti.get(i)));
//            }
//            
//            listaSusjednosti = kopijaListe;
        }

        @Override
        public void run() {
            boolean dretvaGotova = false;
            var Vp = new LinkedHashSet<Integer>(); // vrhovi koje ova dretva vidi
            
            // nulta faza: izvodi se samo jedanput, na samom početku, kako bi
            // svaka dretva odredila svoje vrhove i krenula na posao
            // ovdje isto treba izračunati vjerojatnosti za odabir onih vrhova
            // koje vidi
            int start = id * vrhovaPoDretvi;
            int end = Math.min(start + vrhovaPoDretvi, n);
            for (int i = start; i < end; ++i) {
                Vp.add(i);
            }
            
            try {
                b0.await();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        
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
                    // treća i zadnja faza je uklanjanje X iz V
                    b2dodatno.await();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                } catch (BrokenBarrierException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                }
        
        // određivanje vrhova koje treba eliminirati
        for (int i = start; i < end; ++i) {
            if (X.contains(i)) {
                I.add(i);
                Xstar2.add(i);
                Xstar2.addAll(listaSusjednosti.get(i));
            }
        }
        
                try {
                    b2.await();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                } catch (BrokenBarrierException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                }
        
        Vp.removeAll(Xstar2);
//        for (int v : Xstar2) {
//            Vp.remove(v);
//            V.remove(v);
//        }
        
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
    
    protected static final int dajDefaultBrojDretvi(Graf graf) {
        return Runtime.getRuntime().availableProcessors() >= graf.dajBrojVrhova() ?
            graf.dajBrojVrhova() : Runtime.getRuntime().availableProcessors();
    }
    
    protected void b2kraj() {
        V.removeAll(Xstar2);
    }
    
    protected void b2dodkraj() {
        Xstar2.clear();
        Xstar2.addAll(X);
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
                j--;
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
                j--;
            skupovi.get(j).add(i);
        }
        
        return skupovi;
    }
    
    public CompletableFuture<Collection<Integer>> dajZadatak() {
        return dajZadatak(Executors.newSingleThreadExecutor());
    }
    
    public CompletableFuture<Collection<Integer>> dajZadatak(Executor exec) {
        return CompletableFuture.supplyAsync(() -> {
            return impl();
        }, exec);
    }

    protected Collection<Integer> impl() {
        List<Thread> dretve = new ArrayList<>(brojDretvi);
        int poDretvi = n / brojDretvi;
        
        for (int i = 0; i < brojDretvi; ++i) {
            Thread d = new Thread(new Algoritam1impl(i, poDretvi));
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
