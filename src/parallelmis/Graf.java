package parallelmis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;

/**
 *
 * @author mraguzin
 */
public class Graf {
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
        listaSusjednosti.get(i).add(idx, j);
        idx = Collections.binarySearch(listaSusjednosti.get(j), i);
        listaSusjednosti.get(j).add(idx, i);
        //listaSusjednosti.get(i).add(j);
        //listaSusjednosti.get(j).add(i);
    }
    
    public void ukloniBrid(int i, int j) {
        listaSusjednosti.get(i).remove(Integer.valueOf(j));
        listaSusjednosti.get(j).remove(Integer.valueOf(i));
    }
    
    public void ukloniVrh(int idx) {
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
            if (j < lista.size()) {
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
            //for (var susjed : listaSusjednosti.get(v))
              //  V.remove(susjed);
            V.removeAll(listaSusjednosti.get(v));
            V.remove(v);
        }
        
        return I;
    }
    
    // slijedi nekoliko različitih rješenja temeljenih na paralelnim
    // algoritmima za MIS; svaka metoda koristi svoj vlastiti način višedretvene
    // organizacije posla
    
    public ArrayList<Integer> parallelMIS1() {
        // prema [1]
        // ova implementacija koristi barijere
        final int brojDretvi = Runtime.getRuntime().availableProcessors();
        final ConcurrentLinkedQueue<Integer> V = new ConcurrentLinkedQueue<>(Arrays.asList(dajVrhove()));
        final ConcurrentSkipListSet<Integer> X = new ConcurrentSkipListSet<>();
        final double[] vjerojatnosti = new double[n];
        
        for (int i = 0; i < n; ++i) {
            if (i == 0) {
                if (listaSusjednosti.get(i).isEmpty()) {
                    vjerojatnosti[i] = 0;
                    X.add(i);
                }
                else
                    vjerojatnosti[i] = 1.0 / (2 * listaSusjednosti.get(i).size());
            }
            else {
                if (listaSusjednosti.get(i).isEmpty()) {
                    vjerojatnosti[i] = vjerojatnosti[i-1];
                    X.add(i);
                }
                else
                    vjerojatnosti[i] = vjerojatnosti[i-1] + 1.0 / (2 * listaSusjednosti.get(i).size());
            }
        }
        
        var dio1 = new Runnable() {
            CyclicBarrier b1 = new CyclicBarrier(brojDretvi);
            CyclicBarrier b2 = new CyclicBarrier(brojDretvi);
            @Override
            public void run() {
                
            }
            
        }
    }
    
}
