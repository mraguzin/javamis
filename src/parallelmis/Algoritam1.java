package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    protected final int brojVrhova;
    protected final int brojDretvi;
    protected List<LinkedHashSet<Integer>> particijaVrhova;
    protected Collection<Integer> sviVrhovi;
    protected Collection<Integer> maxNezavisanGraf;
    protected final Set<Integer> nezavisniVrhovi;
    protected final Set<Integer> Xstar2;
    protected List<? extends Collection<Integer>> listaSusjednosti; // read-only
    protected final AtomicInteger brojačParticija; // 0 znači da smo gotovi
    protected final AtomicBoolean gotovo = new AtomicBoolean(false);
    protected final CyclicBarrier barrier0, barrier1, barrier2, barrier2mod, barrier3;
    protected List<LinkedHashSet<Integer>> vrhovi;
    
    protected final int brojIteracija = 1;
    
    public Algoritam1(Graf graf, int brojDretvi) {
        this.graf = graf;
        this.brojVrhova = graf.dajBrojVrhova();
        this.brojDretvi = brojDretvi;
        this.particijaVrhova = particionirajVrhove2(brojDretvi);
        this.sviVrhovi = new ConcurrentSkipListSet<>(Arrays.asList(graf.dajVrhove()));
        this.maxNezavisanGraf = new ConcurrentLinkedQueue<>();
        this.nezavisniVrhovi = new ConcurrentSkipListSet<>();
        this.listaSusjednosti = graf.dajListuSusjednosti();
        this.brojačParticija = new AtomicInteger(brojDretvi);
        this.Xstar2 = new ConcurrentSkipListSet<>();
        
        this.vrhovi = new ArrayList<>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i)
            vrhovi.add(new LinkedHashSet<>());
        
        this.barrier0 = new CyclicBarrier(brojDretvi);
        this.barrier1 = new CyclicBarrier(brojDretvi);
        this.barrier2 = new CyclicBarrier(brojDretvi, this::b2kraj);
        this.barrier3 = new CyclicBarrier(brojDretvi, this::b3kraj);
        this.barrier2mod = new CyclicBarrier(brojDretvi, this::b2dodkraj);
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
            var vrhoviTrenutneDretve = new LinkedHashSet<Integer>();
            
            // nulta faza: izvodi se samo jedanput, na samom početku, kako bi
            // svaka dretva odredila svoje vrhove i krenula na posao
            // ovdje isto treba izračunati vjerojatnosti za odabir onih vrhova
            // koje vidi
            int start = id * vrhovaPoDretvi;
            int end = Math.min(start + vrhovaPoDretvi, brojVrhova);
            for (int i = start; i < end; ++i)
                vrhoviTrenutneDretve.add(i);
            
            try {
                barrier0.await();
            } catch (BrokenBarrierException | InterruptedException ex) {
                Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
            }


            while (!gotovo.getPlain()) {
        // prva faza radi random odabir vrhova iz zadanog podskupa za staviti u skup X
        // svaka odluka odabira je nezavisna od drugih i ima vjerojatnost 1/(2d(v)) za vrh v

                for (int i = 0; i < brojIteracija; ++i) {
                    for (int v : vrhoviTrenutneDretve) {
                        double p = 1.0 / (2.0 * listaSusjednosti.get(v).size());
                        if (p < Double.POSITIVE_INFINITY) {
                            double odabir = ThreadLocalRandom.current().nextDouble();
                            if (p < odabir) nezavisniVrhovi.add(v);
                        }
                        else
                            nezavisniVrhovi.add(v);
                    }
                }

        try {
            barrier1.await();
            // druga faza radi uklanjanje vrhova manjeg stupnja za bridove s vrhovima u X
        } catch (InterruptedException | BrokenBarrierException ex) {
            Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int i : vrhoviTrenutneDretve) {
            for (int j : listaSusjednosti.get(i)) {
                if (sviVrhovi.contains(j) && nezavisniVrhovi.contains(i) && nezavisniVrhovi.contains(j)) {
                    int kojiJeVeći = Integer.compare(listaSusjednosti.get(i).size(), listaSusjednosti.get(j).size());
                    if(kojiJeVeći < 0) nezavisniVrhovi.remove(i);
                    else if (kojiJeVeći > 0) nezavisniVrhovi.remove(j);
                    else nezavisniVrhovi.remove(Math.min(i, j));
                }
            }
        }

        // treća i zadnja faza je uklanjanje svih vrhova nezavisnog grafa i njihovih susjeda iz sviVrhovi
        
                try {
                    // treća i zadnja faza je uklanjanje X iz V
                    barrier2mod.await();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                } catch (BrokenBarrierException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                }
        
        // određivanje vrhova koje treba eliminirati
        for (int i = start; i < end; ++i) {
            if (nezavisniVrhovi.contains(i)) {
                maxNezavisanGraf.add(i);
                Xstar2.add(i);
                Xstar2.addAll(listaSusjednosti.get(i));
            }
        }
        
                try {
                    barrier2.await();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                } catch (BrokenBarrierException ex) {
                    Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
                }

        vrhoviTrenutneDretve.removeAll(Xstar2);

        if (!dretvaGotova && vrhoviTrenutneDretve.isEmpty()) {
            dretvaGotova = true;
            brojačParticija.decrementAndGet();
        }

            try {
                barrier3.await();
            } catch (InterruptedException | BrokenBarrierException ex) {
                Logger.getLogger(Algoritam1v1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        }
    }
    
    protected static int dajDefaultBrojDretvi(Graf graf) {
        return Math.min(Runtime.getRuntime().availableProcessors(), graf.dajBrojVrhova());
    }
    
    protected void b2kraj() {
        sviVrhovi.removeAll(Xstar2);
    }
    
    protected void b2dodkraj() {
        Xstar2.clear();
        Xstar2.addAll(nezavisniVrhovi);
    }
    
    protected void b3kraj() {
        nezavisniVrhovi.clear();
        
        if (brojačParticija.getPlain() == 0)
            gotovo.set(true);
    }
    
    protected ArrayList<TreeSet<Integer>> particionirajVrhove(int brojDretvi) {
        var skupovi = new ArrayList<TreeSet<Integer>>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i)
            skupovi.add(new TreeSet<>());
        int ukupnoParticija = brojVrhova / brojDretvi;
        for (int i = 0; i < brojVrhova; ++i) {
            int trenutnaParticija = i / ukupnoParticija;
            if (trenutnaParticija == brojDretvi) trenutnaParticija--;
            //if (skupovi.get(j) == null)
                //skupovi.set(j, new TreeSet<>());
            skupovi.get(trenutnaParticija).add(i);
        }
        
        return skupovi;
    }
        
    // sigh...
    protected List<LinkedHashSet<Integer>> particionirajVrhove2(int brojDretvi) {
        var skupovi = new ArrayList<LinkedHashSet<Integer>>(brojDretvi);
        for (int i = 0; i < brojDretvi; ++i)
            skupovi.add(new LinkedHashSet<>());
        int ukupnoParticija = brojVrhova / brojDretvi;
        for (int i = 0; i < brojVrhova; ++i) {
            int trenutnaParticija = i / ukupnoParticija;
            if (trenutnaParticija == brojDretvi) trenutnaParticija--;
            skupovi.get(trenutnaParticija).add(i);
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
        int poDretvi = brojVrhova / brojDretvi;
        
        for (int i = 0; i < brojDretvi; ++i) {
            Thread dretva = new Thread(new Algoritam1impl(i, poDretvi));
            dretve.add(dretva);
            dretva.start();
        }
        
        for (Thread dretva : dretve)
            try {
                dretva.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Algoritam1.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return maxNezavisanGraf;
    }
}
