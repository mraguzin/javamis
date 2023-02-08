package parallelmis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;

/**
 *
 * @author mraguzin
 * [1] <a href="https://disco.ethz.ch/alumni/pascalv/refs/mis_1986_luby.pdf">...</a>
 */
public class Graf { // graf je napravljen da bude mutabilan tako da se lako
    // modificira od strane GUI-ja i dalje smatra istim objektom, ali zbog
    // paralelizma moramo osigurati da se nikako ne može mijenjati tijekom
    // samog izvođenja paralelnih rješenja za MIS!
    private int brojVrhova;
    private ArrayList<ArrayList<Integer>> listaSusjednosti = new ArrayList<>();
    
    public int dajBrojVrhova() {
        return brojVrhova;
    }
    
    public ArrayList<ArrayList<Integer>> dajListuSusjednosti() {
        return listaSusjednosti;
    }

    public ArrayList<Integer> dajListuSusjednostiKruga(Integer index) { return listaSusjednosti.get(index); }

    public void dodajVrh() {
        listaSusjednosti.add(new ArrayList<>());
        ++brojVrhova;
    }
    
    public void dodajBrid(int i, int j) {
        // ako želimo koristiti binarno pretraživanje ispod, moramo paziti da ubacujemo
        // indekse susjeda na pravo mjesto
        int idx = Collections.binarySearch(dajListuSusjednostiKruga(i), j);
        if (idx >= 0) return; // ne dupliciramo bridove
        dajListuSusjednostiKruga(i).add(-idx-1, j);
        idx = Collections.binarySearch(dajListuSusjednostiKruga(j), i);
        dajListuSusjednostiKruga(j).add(-idx-1, i);
    }
    
    public void ukloniBrid(int i, int j) {
        dajListuSusjednostiKruga(i).remove(j);
        dajListuSusjednostiKruga(j).remove(i);
    }
    
    public void ukloniVrh(int idx) { // PAZI: ovo efektivno *renumerira* vrhove --- bitno za GUI impl.!
        // prvo uklonimo sve incidentne bridove
        for (int susjed : dajListuSusjednostiKruga(idx)) {
            int indexZaMaknuti = Collections.binarySearch(dajListuSusjednostiKruga(susjed), idx);
            dajListuSusjednostiKruga(susjed).remove(indexZaMaknuti);
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
        
        brojVrhova--;
        
        System.out.println(listaSusjednosti);
    }
    
    public static Graf stvoriIzDatoteke(File f) throws Exception {
        Graf graf;
        // datoteka mora biti u tekstualnom Challenge 9 formatu
        // vidjeti https://www.diag.uniroma1.it//challenge9/download.shtml
        try (java.util.Scanner scanner = new Scanner(f)) {
            String linija;
            graf = new Graf();
            int brojBridova = 0, brojVrhova;
            boolean učitanProlog = false;
            int k = 0;
            
            while (scanner.hasNextLine()) {
                linija = scanner.nextLine();
                String[] dijelovi = linija.split(" ", 4);
                switch (dijelovi[0]) {
                    case "c":
                        break;
                    case "p":
                    {
                        if (učitanProlog) {
                            throw new Exception("Više puta se pojavljuje 'p' unos!");
                        }
                        brojVrhova = Integer.parseInt(dijelovi[2]);
                        brojBridova = Integer.parseInt(dijelovi[3]);
                        for (int i = 0; i < brojVrhova; ++i)
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
                        if (!učitanProlog || k < brojBridova)
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
        Integer[] array = new Integer[brojVrhova];
        for (int i = 0; i < brojVrhova; ++i)
            array[i] = i;
        
        return array;
    }
    
    // "naivno" sekvencijalno rješenje MIS problema
    public ArrayList<Integer> sequentialMIS() {
        ArrayList<Integer> maxNezavisanGraf = new ArrayList<>();
        HashSet<Integer> sviVrhovi = new HashSet<>(Arrays.asList(dajVrhove()));
        
        while (!sviVrhovi.isEmpty()) {
            int v = sviVrhovi.iterator().next();
            maxNezavisanGraf.add(v);
            dajListuSusjednostiKruga(v).forEach(sviVrhovi::remove);
            sviVrhovi.remove(v);
        }
        
        return maxNezavisanGraf;
    }

}
