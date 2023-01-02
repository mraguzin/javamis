package parallelmis;

import java.util.ArrayList;
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
        //ArrayList<Integer> expResult = null;
        ArrayList<Integer> result = instance.sequentialMIS();
        //assertEquals(expResult, result);
        System.out.println(result.toString());
        
        var future = instance.parallelMIS1();
        future.run();
        result = future.get();
        System.out.println(result.toString());        
        
    }
}
