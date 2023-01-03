package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import parallelmis.helpers.SharedDouble;

/**
 *
 * @author mraguzin
 */
public class Graf { // graf je napravljen da bude mutabilan tako da se lako
    // modificira od strane GUI-ja i dalje smatra istim objektom, ali zbog
    // paralelizma moramo osigurati da se nikako ne može mijenjati tijekom
    // samog izvođenja paralelnih rješenja za MIS!
    private int n; // broj vrhova; V={0,1,...,n-1}
    private ArrayList<ArrayList<Integer>> listaSusjednosti = new ArrayList<>();
    
    public void dodajVrh() {
        listaSusjednosti.add(new ArrayList<>());
        ++n;
    }
    
    public void dodajBrid(int i, int j) {
        // ako želimo koristiti binarno pretraživanje ispod, moramo paziti da ubacujemo
        // indekse susjeda na pravo mjesto
        int idx = Collections.binarySearch(listaSusjednosti.get(i), j);
        listaSusjednosti.get(i).add(-idx-1, j);
        idx = Collections.binarySearch(listaSusjednosti.get(j), i);
        listaSusjednosti.get(j).add(-idx-1, i);
    }
    
    public void ukloniBrid(int i, int j) {
        listaSusjednosti.get(i).remove(Integer.valueOf(j));
        listaSusjednosti.get(j).remove(Integer.valueOf(i));
    }
    
    public void ukloniVrh(int idx) { // PAZI: ovo efektivno *renumerira* vrhove --- bitno za GUI impl.!
        // prvo uklonimo sve incidentne bridove
        for (var susjed : listaSusjednosti.get(idx)) {
            int i = Collections.binarySearch(listaSusjednosti.get(susjed), idx);
            listaSusjednosti.get(susjed).remove(i);
            //listaSusjednosti.get(susjed).remove(Integer.valueOf(idx)); // Možda brže ako se koristi binarno? Uočimo da su liste
            // susjeda uvijek sortirane
        }
        listaSusjednosti.remove(idx);
        
        // korigirajmo indekse susjeda za sve vrhove indeksa < idx
        ArrayList<Integer> tmp1[] = new ArrayList[1];
        tmp1 = listaSusjednosti.toArray(tmp1);
        for (int i = 0; i < idx; ++i) {
            //var lista = listaSusjednosti.get(i);
            var lista = tmp1[i];
            int j = Collections.binarySearch(lista, idx);
            if (j >= 0) {
                Integer[] tmp2 = new Integer[lista.size()];
                tmp2 = lista.toArray(tmp2);
                for (int k = j; k < lista.size(); ++k)
                    tmp2[k]--;
                
                var l = Arrays.asList(tmp2);
                //listaSusjednosti.remove(i);
                //listaSusjednosti.add(i, new ArrayList<>(l));
                tmp1[i] = new ArrayList<>(l);
            }
        }
        
        var l = Arrays.asList(tmp1);
        listaSusjednosti = new ArrayList<>(l);
        n--;
    }
    
    public static void ispišiMatricu(float[][] m) {
        for (var red : m)
            System.out.println(Arrays.toString(red));
    }
    
    public Integer[] dajVrhove() {
        Integer[] arr = new Integer[n];
        for (int i = 0; i < n; ++i)
            arr[i] = i;
        
        return arr;
    }
    
    // "naivno" sekvencijalno rješenje MIS problema
    public ArrayList<Integer> sequentialMIS() {
        ArrayList<Integer> I = new ArrayList<>();
        HashSet<Integer> V = new HashSet<>(Arrays.asList(dajVrhove()));
        
        while (!V.isEmpty()) {
            int v = V.iterator().next();
            I.add(v);
            V.removeAll(listaSusjednosti.get(v));
            V.remove(v);
        }
        
        return I;
    }
    
    private TreeSet<Integer>[] particionirajVrhove(int k) {
        TreeSet<Integer>[] skupovi = new TreeSet[k];
        int m = n / k;
        for (int i = 0; i < n; ++i) {
            int j = i / m;
            if (j == k)
                k--;
            if (skupovi[j] == null)
                skupovi[j] = new TreeSet<>();
            skupovi[j].add(i);
        }
        
        return skupovi;
    }
    
    public FutureTask<ArrayList<Integer>> parallelMIS1() {
        var c = (Callable<ArrayList<Integer>>) this::parallelMIS1impl;        
        return new FutureTask<>(c);
    }
    
    // slijedi nekoliko različitih rješenja temeljenih na paralelnim
    // algoritmima za MIS; svaka metoda koristi svoj vlastiti način višedretvene
    // organizacije posla
    
    private ArrayList<Integer> parallelMIS1impl() {
        // stroga implementacija algoritma iz [1], str. 3
        // ova implementacija koristi barijere
        final int brojDretvi = n/Runtime.getRuntime().availableProcessors() == 0 ? n : n/Runtime.getRuntime().availableProcessors();
        final TreeSet<Integer> Vp[] = particionirajVrhove(brojDretvi);
        final ConcurrentSkipListSet<Integer> V = new ConcurrentSkipListSet<>(Arrays.asList(dajVrhove()));
        final ArrayList<Integer> I = new ArrayList<>();
        final ConcurrentSkipListSet<Integer> X = new ConcurrentSkipListSet<>();
        final double[] vjerojatnosti = new double[n];
        final AtomicInteger brojačParticija = new AtomicInteger(brojDretvi); // 0 znači da smo gotovi
        final AtomicBoolean gotovo = new AtomicBoolean(false);
        
        for (int i = 0; i < n; ++i) {
            vjerojatnosti[i] = 1.0 / (2.0 * listaSusjednosti.get(i).size()); // tu može doći ∞ i to je ok
        }
        
        System.out.println("vjerojatnosti:" + Arrays.toString(vjerojatnosti));
        
        final TreeSet<Integer>[] vrhovi = new TreeSet[brojDretvi]; // ideja je
        // da ako koristimo sortiran skup, možemo u optimalnom vremenu izračunati
        // presjeke i sl. Jesmo li sigurni da Java koristi takvu implementaciju
        // u tom posebnom slučaju (kada su sve kolekcije sortirane)? Mogli bi
        // jednostavno implementirati to preko polja, koristeći funkciju iz
        // klase Pomoćne
        
        Runnable b2kraj = () -> {
            var Xstar = new TreeSet<Integer>(X);
            for (int v : X)
                Xstar.addAll(listaSusjednosti.get(v));
            for (int i = 0; i < brojDretvi; ++i) {
                vrhovi[i] = new TreeSet<>(Vp[i]);
                vrhovi[i].retainAll(Xstar); // Vp ∩ X*
            }
            
            I.addAll(X);
        };
        
        Runnable b3kraj = () -> {
            X.clear();
            System.out.println("kraj faze3; V="+V.toString());
            System.out.println("I="+I.toString());
            
            if (brojačParticija.getPlain() == 0)
                gotovo.set(true);
        };
            
        CyclicBarrier b1 = new CyclicBarrier(brojDretvi);
        CyclicBarrier b2 = new CyclicBarrier(brojDretvi, b2kraj);
        CyclicBarrier b3 = new CyclicBarrier(brojDretvi, b3kraj);
        List<Thread> dretve = new ArrayList<>(brojDretvi);
        
        for (int i = 0; i < brojDretvi - 1; ++i) {
            Thread d = new Thread(new Algoritam1v1(brojDretvi, i, Vp[i].size(),
            Vp[i], V, X, listaSusjednosti, vjerojatnosti, vrhovi, gotovo,
                    brojačParticija, b1, b2, b3));
            dretve.add(d);
            d.start();
        }
        Thread d = new Thread(new Algoritam1v1(brojDretvi, brojDretvi-1,
                Vp[brojDretvi-1].size(), Vp[brojDretvi-1], V, X, listaSusjednosti,
                vjerojatnosti, vrhovi, gotovo, brojačParticija, b1, b2, b3));
        dretve.add(d);
        d.start();
        
        for (Thread i : dretve)
            try {
                i.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Graf.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return I;        
    }
    
    public FutureTask<ArrayList<Integer>> parallelMIS2() {
        var c = (Callable<ArrayList<Integer>>) this::parallelMIS2impl;        
        return new FutureTask<>(c);
    }
    
    private ArrayList<Integer> parallelMIS2impl() {
        // stroga implementacija algoritma iz [1], str. 3
        // ova implementacija koristi barijere i finije lokote
        final int brojDretvi = n/Runtime.getRuntime().availableProcessors() == 0 ? n : n/Runtime.getRuntime().availableProcessors();
        final TreeSet<Integer> Vp[] = particionirajVrhove(brojDretvi);
        final LinkedHashSet<Integer> V = new LinkedHashSet<>(Arrays.asList(dajVrhove()));
        final ArrayList<Integer> I = new ArrayList<>();
        final ConcurrentSkipListSet<Integer> X = new ConcurrentSkipListSet<>();
        final double[] vjerojatnosti = new double[n];
        final AtomicInteger brojačParticija = new AtomicInteger(brojDretvi); // 0 znači da smo gotovi
        final AtomicBoolean gotovo = new AtomicBoolean(false);
        final ReadWriteLock lokot = new ReentrantReadWriteLock();
        final Lock lokotR = lokot.readLock();
        final Lock lokotW = lokot.writeLock();
        
        for (int i = 0; i < n; ++i) {
            vjerojatnosti[i] = 1.0 / (2.0 * listaSusjednosti.get(i).size()); // tu može doći ∞ i to je ok
        }
        
        System.out.println("vjerojatnosti:" + Arrays.toString(vjerojatnosti));
        
        final TreeSet<Integer>[] vrhovi = new TreeSet[brojDretvi]; // ideja je
        // da ako koristimo sortiran skup, možemo u optimalnom vremenu izračunati
        // presjeke i sl. Jesmo li sigurni da Java koristi takvu implementaciju
        // u tom posebnom slučaju (kada su sve kolekcije sortirane)? Mogli bi
        // jednostavno implementirati to preko polja, koristeći funkciju iz
        // klase Pomoćne
        
        Runnable b2kraj = () -> {
            var Xstar = new TreeSet<Integer>(X);
            for (int v : X)
                Xstar.addAll(listaSusjednosti.get(v));
            for (int i = 0; i < brojDretvi; ++i) {
                vrhovi[i] = new TreeSet<>(Vp[i]);
                vrhovi[i].retainAll(Xstar); // Vp ∩ X*
            }
            
            I.addAll(X);
        };
        
        Runnable b3kraj = () -> {
            X.clear();
            System.out.println("kraj faze3; V="+V.toString());
            System.out.println("I="+I.toString());
            
            if (brojačParticija.getPlain() == 0)
                gotovo.set(true);
        };
            
        CyclicBarrier b1 = new CyclicBarrier(brojDretvi);
        CyclicBarrier b2 = new CyclicBarrier(brojDretvi, b2kraj);
        CyclicBarrier b3 = new CyclicBarrier(brojDretvi, b3kraj);
        List<Thread> dretve = new ArrayList<>(brojDretvi);
        
        for (int i = 0; i < brojDretvi - 1; ++i) {
            Thread d = new Thread(new Algoritam1v2(brojDretvi, i, Vp[i].size(),
            Vp[i], V, X, listaSusjednosti, vjerojatnosti, vrhovi, gotovo,
                    brojačParticija, b1, b2, b3, lokotR, lokotW));
            dretve.add(d);
            d.start();
        }
        Thread d = new Thread(new Algoritam1v2(brojDretvi, brojDretvi-1,
                Vp[brojDretvi-1].size(), Vp[brojDretvi-1], V, X, listaSusjednosti,
                vjerojatnosti, vrhovi, gotovo, brojačParticija, b1, b2, b3,
        lokotR, lokotW));
        
        dretve.add(d);
        d.start();
        
        for (Thread i : dretve)
            try {
                i.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Graf.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return I;        
    }
    
}
