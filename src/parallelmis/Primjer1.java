package parallelmis;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author mraguzin
 */
public class Primjer1 {
    
    public static void main(String args[]) throws InterruptedException, ExecutionException {
        Graf instance = new Graf();
        instance.dodajVrh();
        instance.dodajVrh();  //   *----*-----*
        instance.dodajVrh();
        instance.dodajBrid(0, 1);
        instance.dodajBrid(1, 2);

        var result = instance.sequentialMIS();
        //assertEquals(expResult, result);
        System.out.println(result.toString());
        
        var future = new Algoritam1v1(instance).dajZadatak();
        future.thenAccept((rez) -> System.out.println(rez.toString()));
        
        future = new Algoritam1v2(instance).dajZadatak();
        future.thenAccept((rez) -> System.out.println(rez.toString()));
        
        future = new Algoritam1v3(instance).dajZadatak();
        future.thenAccept((rez) -> System.out.println(rez.toString()));
    }
}
