package parallelmis;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;

/**
 *
 * @author mraguzin
 */
public class Primjer3 {
    public static void main(String[] args) throws Exception {
        final StopWatch stopWatch = new StopWatch();

        var file = new File("mis_graf1.gr"); // poveliki graf, ali trebao bi nam i veći
        Graf graf = Graf.stvoriIzDatoteke(file);
        stopWatch.start();
        var rez = graf.sequentialMIS();
        stopWatch.stop();
        double time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        System.out.println(rez.toString());
        System.out.println("seq time=" + time + " ms");

        var exec = Executors.newSingleThreadExecutor();

        stopWatch.start();
        var zadatak = new Algoritam1v4(graf).dajZadatak(exec);
        var rez2 = zadatak.get();
        stopWatch.stop();
        time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        System.out.println("parv4 time=" + time + " ms");
        stopWatch.reset();

        stopWatch.start();
        zadatak = new Algoritam1v3(graf).dajZadatak(exec);
        rez2 = zadatak.get();
        stopWatch.stop();
        time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        System.out.println("parv3 time=" + time + " ms");
        stopWatch.reset();

        stopWatch.start();
        zadatak = new Algoritam1v2(graf).dajZadatak(exec);
        rez2 = zadatak.get();
        stopWatch.stop();
        time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        System.out.println("parv2 time=" + time + " ms");
        stopWatch.reset();

        stopWatch.start();
        zadatak = new Algoritam1v1(graf).dajZadatak(exec);
        rez2 = zadatak.get();
        stopWatch.stop();
        time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        System.out.println("parv1 time=" + time + " ms");
        stopWatch.reset();

        exec.shutdownNow();
    }

}
