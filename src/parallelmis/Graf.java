package parallelmis;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author mraguzin
 */
public class Graf {
    private final int n; // broj vrhova
    private List<Trojka<Integer,Integer,Float>> listaSusjednosti; // (i,j,težina)
    private float[][] matricaSusjednosti; // konvencija je da težina ∞ označava
    //nepostojanje brida
    
    // konstrukcija preko liste susjednosti
    public Graf(List<Trojka<Integer,Integer,Float>> listaSusjednosti) {
        n = listaSusjednosti.size();
        // provjera validnosti ulaza
        for (var trojka : listaSusjednosti) {
            if (trojka.prvi < 0 || trojka.prvi >= n ||
                    trojka.drugi < 0 || trojka.drugi >= n ||
                    trojka.prvi.equals(trojka.drugi) && trojka.treći != 0) // petlje nisu dozvoljene!
                throw new IllegalArgumentException(trojka.toString());
        }
        
        this.listaSusjednosti = listaSusjednosti;
    }
    
    // konstrukcija preko matrice susjednosti
    public Graf(float[][] matricaSusjednosti) {
        n = matricaSusjednosti.length;
        // provjera validnosti ulaza
        for (int i = 0; i < n; ++i) {
            if (matricaSusjednosti[i].length != n)
                throw new IllegalArgumentException("Matrica nije kvadratna!");
            if (matricaSusjednosti[i][i] != 0)
                throw new IllegalArgumentException("("+i+","+i+")="+
            matricaSusjednosti[i][i]+", a mora biti jednak 0");
        }
        
        this.matricaSusjednosti = matricaSusjednosti; //
        //ovdje svakako nema smisla dozvoliti petlje jer one ili samo produljuju
        //bilo koji put kroz dani vrh ili dovode do toga da je optimalno rješenje
        //beskonačno petljanje!
    }
    
    public float[][] dajMatricuSusjednosti() {
        if (matricaSusjednosti != null)
            return matricaSusjednosti;
        // pretvorimo graf u matričnu reprezentaciju
        matricaSusjednosti = new float[n][n];
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (i == j)
                    matricaSusjednosti[i][i] = 0;
                else
                    matricaSusjednosti[i][j] = Float.POSITIVE_INFINITY;
            }
        }
        
        for (var trojka : listaSusjednosti)
            matricaSusjednosti[trojka.prvi][trojka.drugi] = trojka.treći;
        
        return matricaSusjednosti;
    }
    
    public static void ispišiMatricu(float[][] m) {
        for (var red : m)
            System.out.println(Arrays.toString(red));
    }
    
    // Floyd-Warshallov algoritam
    public float[][] najkraćiPut(boolean ispisKoraka) { // vraća matricu duljina najkraćih puteva između svih parova vrhova
        // prema [1]
        dajMatricuSusjednosti();
        float[][] m = new float[n][];
        for (int i = 0; i < n; ++i)
            m[i] = matricaSusjednosti[i].clone();
        
        for (int k = 0; k < n; ++k) {
            for (int i = 0; i < n; ++i) {
                if (m[i][k] < Float.POSITIVE_INFINITY) {
                    for (int j = 0; j < n; ++j) {
                        if (m[k][j] < Float.POSITIVE_INFINITY) {
                            float s = m[i][k] + m[k][j];
                            if (s < m[i][j])
                                m[i][j] = s;
                        }
                    }
                }
            }
            
            if (ispisKoraka) {
                System.out.println("k="+k+":");
                ispišiMatricu(m);
            }
        }
        
        return m;
    }
    
}
