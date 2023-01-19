package parallelmis;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
//import org.apache.commons.lang3.time.StopWatch;
//
///**
// *
// * @author mraguzin
// */
//public class Primjer3 {
//    public static void main(String[] args) throws Exception {
//        final StopWatch sw = new StopWatch();
//
//        var f = new File("mis_graf1.gr"); // poveliki graf, ali trebao bi nam i veći
//        Graf g = Graf.stvoriIzDatoteke(f);
//        sw.start();
//        var rez = g.sequentialMIS();
//        sw.stop();
//        double t = sw.getTime(TimeUnit.MILLISECONDS);
//        sw.reset();
//        System.out.println(rez.toString());
//        System.out.println("seq t=" + t + " ms");
//
//        var exec = Executors.newSingleThreadExecutor();
//
//        sw.start();
//        var zadatak = new Algoritam1v4(g).dajZadatak(exec);
//        var rez2 = zadatak.get();
//        sw.stop();
//        t = sw.getTime(TimeUnit.MILLISECONDS);
//        System.out.println("parv4 t=" + t + " ms");
//        sw.reset();
//
//        sw.start();
//        zadatak = new Algoritam1v3(g).dajZadatak(exec);
//        rez2 = zadatak.get();
//        sw.stop();
//        t = sw.getTime(TimeUnit.MILLISECONDS);
//        System.out.println("parv3 t=" + t + " ms");
//        sw.reset();
//
//        sw.start();
//        zadatak = new Algoritam1v2(g).dajZadatak(exec);
//        rez2 = zadatak.get();
//        sw.stop();
//        t = sw.getTime(TimeUnit.MILLISECONDS);
//        System.out.println("parv2 t=" + t + " ms");
//        sw.reset();
//
//        sw.start();
//        zadatak = new Algoritam1v1(g).dajZadatak(exec);
//        rez2 = zadatak.get();
//        sw.stop();
//        t = sw.getTime(TimeUnit.MILLISECONDS);
//        System.out.println("parv1 t=" + t + " ms");
//        sw.reset();
//
//        exec.shutdownNow();
//    }
//
//}
