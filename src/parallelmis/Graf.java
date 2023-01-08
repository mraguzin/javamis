package parallelmis;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mraguzin
 * [1] https://disco.ethz.ch/alumni/pascalv/refs/mis_1986_luby.pdf
 */
public class Graf { // graf je napravljen da bude mutabilan tako da se lako
    // modificira od strane GUI-ja i dalje smatra istim objektom, ali zbog
    // paralelizma moramo osigurati da se nikako ne može mijenjati tijekom
    // samog izvođenja paralelnih rješenja za MIS!
    private int n; // broj vrhova; V={0,1,...,n-1}
    private ArrayList<ArrayList<Integer>> listaSusjednosti = new ArrayList<>();
    
    public int dajBrojVrhova() {
        return n;
    }
    
    public ArrayList<ArrayList<Integer>> dajListu() {
        return listaSusjednosti;
    }
    
    public void dodajVrh() {
        listaSusjednosti.add(new ArrayList<>());
        ++n;
    }
    
    public void dodajBrid(int i, int j) {
        // ako želimo koristiti binarno pretraživanje ispod, moramo paziti da ubacujemo
        // indekse susjeda na pravo mjesto
        int idx = Collections.binarySearch(listaSusjednosti.get(i), j);
        if (idx >= 0)
            return; // ne dupliciramo bridove
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
        for (int susjed : listaSusjednosti.get(idx)) {
            int i = Collections.binarySearch(listaSusjednosti.get(susjed), idx);
            listaSusjednosti.get(susjed).remove(i);
        }
        listaSusjednosti.remove(idx);
        
        // korigirajmo indekse susjeda za sve vrhove indeksa < idx
        ArrayList<Integer> tmp1[] = new ArrayList[1];
        tmp1 = listaSusjednosti.toArray(tmp1);
        for (int i = 0; i < tmp1.length; ++i) {
            var lista = tmp1[i];
            if (lista == null)
                continue;
            int j = Collections.binarySearch(lista, idx);
            if (-j-1 < lista.size() && -j-1 >= 0) {
                Integer[] tmp2 = new Integer[lista.size()];
                tmp2 = lista.toArray(tmp2);
                for (int k = -j-1; k < lista.size(); ++k)
                    tmp2[k]--;
                
                var l = Arrays.asList(tmp2);
                System.out.println("tmp2 "+Arrays.toString(tmp2));
                tmp1[i] = new ArrayList<>(l);
            }
        }
        
        var l = Arrays.asList(tmp1);
        listaSusjednosti = new ArrayList<>(l);
        if (listaSusjednosti.get(0) == null)
            listaSusjednosti = new ArrayList<>();
        
        n--;
        
        System.out.println(listaSusjednosti.toString());
    }
    
    public static Graf stvoriIzDatoteke(File f) throws Exception {
        Graf graf;
        try ( // datoteka mora biti u tekstualnom Challenge 9 formatu
        // vidjeti https://www.diag.uniroma1.it//challenge9/download.shtml
        java.util.Scanner s = new Scanner(f)) {
            String linija;
            graf = new Graf();
            int bridova = 0, vrhova;
            boolean učitanProlog = false;
            int k = 0;
            
            while (s.hasNextLine()) {
                linija = s.nextLine();
                String[] dijelovi = linija.split(" ", 4);
                switch (dijelovi[0]) {
                    case "c":
                        break;
                    case "p":
                    {
                        if (učitanProlog)
                            throw new Exception("Više puta se pojavljuje 'a' unos!");
                        vrhova = Integer.parseInt(dijelovi[2]);
                        bridova = Integer.parseInt(dijelovi[3]);
                        for (int i = 0; i < vrhova; ++i)
                            graf.dodajVrh();
                        učitanProlog = true;
                        break;
                    }
                    case "a":
                    {
                        ++k;
                        int i = Integer.parseInt(dijelovi[1]) - 1;
                        int j = Integer.parseInt(dijelovi[2]) - 1;
                        graf.dodajBrid(i, j);
                        break;
                    }
                    default:
                        if (!učitanProlog || k < bridova)
                            throw new Exception("Kriva oznaka linije");
                }
            }
        }
        return graf;
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
        
    private LinkedHashSet<Integer>[] particionirajVrhove2(int k) {
        LinkedHashSet<Integer>[] skupovi = new LinkedHashSet[k];
        int m = n / k;
        for (int i = 0; i < n; ++i) {
            int j = i / m;
            if (j == k)
                k--;
            if (skupovi[j] == null)
                skupovi[j] = new LinkedHashSet<>();
            skupovi[j].add(i);
        }
        
        return skupovi;
    }
    
    // slijedi nekoliko različitih rješenja temeljenih na paralelnim
    // algoritmima za MIS; svaka metoda koristi svoj vlastiti način višedretvene
    // organizacije posla
    
    public FutureTask<List<Integer>> parallelMIS5() {
        var c = (Callable<List<Integer>>) this::parallelMIS5impl;
        return new FutureTask<>(c);
    }
    
    private List<Integer> parallelMIS5impl() {
        final int brojDretvi = Runtime.getRuntime().availableProcessors() >= n ? n : Runtime.getRuntime().availableProcessors();
        final LinkedHashSet<Integer> Vp[] = particionirajVrhove2(brojDretvi);
        final ConcurrentSkipListSet<Integer> V = new ConcurrentSkipListSet<>(Arrays.asList(dajVrhove()));
        final ArrayList<Integer> I = new ArrayList<>();
        final ConcurrentSkipListSet<Integer> X = new ConcurrentSkipListSet<>();
        final LinkedHashSet<Integer> Xstar = new LinkedHashSet<>();
        final AtomicBoolean gotovo = new AtomicBoolean(false);
        final ArrayList<LinkedHashSet<Integer>> kopijaListe = new ArrayList<>(n); // kopija liste susjednosti
        final AtomicLong gotoveDretve = new AtomicLong(); // 64-bitno polje; bit i je 1 akko i-ta dretva
        // nema više posla i traži da joj se dodijele neki od preostalih vrhova iz V
        
        for (int i = 0; i < n; ++i) {
            kopijaListe.add(new LinkedHashSet<>(listaSusjednosti.get(i)));
        }
        
        Runnable b2kraj = () -> {
            for (int v : X) {
                I.add(v);
                Xstar.add(v);
                Xstar.addAll(kopijaListe.get(v));
            }
        };
        
        Runnable b3kraj = () -> {
            V.removeAll(Xstar);
            X.clear();
            Xstar.clear();
            
            if (V.isEmpty())
                gotovo.set(true);
            else {
                long tmp = gotoveDretve.getPlain();
                int gotova;
                int aktivna;
                long rez = tmp;
                int limit = 0;
                
                    while (tmp != 0) {
                        gotova = Long.numberOfTrailingZeros(tmp);
                        if (gotova == 0)
                            aktivna = Long.numberOfTrailingZeros(~tmp);
                        else
                            aktivna = gotova - 1;
                        
                        if (aktivna >= brojDretvi || gotova >= brojDretvi)
                            break;
                        
                        int vrhova = Vp[aktivna].size();
                        if (vrhova >= 2) {
                            limit = gotova > limit ? gotova : limit;
                            var it = Vp[aktivna].iterator();
                            for (int i = 0; i < vrhova/2; ++i) {
                                int el = it.next();
                                Vp[gotova].add(el);
                                Vp[aktivna].remove(el);
                            }
                            
                            rez &= ~(1l << gotova);
                        }
                        
                        tmp &= ~(1l << gotova);
                    }
                    
                    gotoveDretve.setPlain(rez);
                }
        };
            
        CyclicBarrier b1 = new CyclicBarrier(brojDretvi);
        CyclicBarrier b2 = new CyclicBarrier(brojDretvi, b2kraj);
        CyclicBarrier b3 = new CyclicBarrier(brojDretvi, b3kraj);
        List<Thread> dretve = new ArrayList<>(brojDretvi);
        
        for (int i = 0; i < brojDretvi - 1; ++i) {
            Thread d = new Thread(new Algoritam1v5(brojDretvi, i, Vp[i].size(),
            Vp[i], V, X, kopijaListe, Xstar, gotovo, gotoveDretve, b1, b2, b3));
            dretve.add(d);
            d.start();
        }
        Thread d = new Thread(new Algoritam1v5(brojDretvi, brojDretvi-1,
                Vp[brojDretvi-1].size(), Vp[brojDretvi-1], V, X, kopijaListe,
                Xstar, gotovo, gotoveDretve, b1, b2, b3));
        dretve.add(d);
        d.start();
        
        for (Thread i : dretve)
            try {
                i.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Graf.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return List.copyOf(I);
    }
    
}
