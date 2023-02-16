package parallelmis;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Test 2
 * @author mraguzin
 */
public class Primjer2 {
    
    public static void main(String[] args) {
        Graf kocka = new Graf();
        kocka.dodajVrh();
        kocka.dodajVrh();
        kocka.dodajVrh();
        kocka.dodajVrh();
        kocka.dodajVrh();
        kocka.dodajVrh();
        kocka.dodajVrh();
        kocka.dodajVrh();
        
        kocka.dodajBrid(0, 1);
        kocka.dodajBrid(0, 2);
        kocka.dodajBrid(1, 3);
        kocka.dodajBrid(2, 3);
        kocka.dodajBrid(0, 4);
        kocka.dodajBrid(4, 5);
        kocka.dodajBrid(5, 1);
        kocka.dodajBrid(4, 6);
        kocka.dodajBrid(6, 2);
        kocka.dodajBrid(6, 7);
        kocka.dodajBrid(7, 5);
        kocka.dodajBrid(3, 7);
        
        List<Integer> result = kocka.sequentialMIS();
        System.out.println(result.toString());
        
        var exec = Executors.newSingleThreadExecutor();
        var future = new Algoritam1v2(kocka).dajZadatak(exec);
        future.thenAccept((rez) -> System.out.println(rez.toString()));
        
        future = new Algoritam1v3(kocka).dajZadatak(exec);
        future.thenAccept((rez) -> System.out.println(rez.toString()));
        
        future = new Algoritam1v1(kocka).dajZadatak(exec);
        future.thenAccept((rez) -> {
            System.out.println(rez.toString());
            exec.shutdownNow();
                });
    }
}
