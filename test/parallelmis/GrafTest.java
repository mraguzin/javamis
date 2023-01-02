package parallelmis;

import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author mraguzin
 */
public class GrafTest {
    
    public GrafTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of dodajVrh method, of class Graf.
     */
    @Test
    public void testDodajVrh() {
        System.out.println("dodajVrh");
        Graf instance = new Graf();
        instance.dodajVrh();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of dodajBrid method, of class Graf.
     */
    @Test
    public void testDodajBrid() {
        System.out.println("dodajBrid");
        int i = 0;
        int j = 0;
        Graf instance = new Graf();
        instance.dodajBrid(i, j);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of ukloniBrid method, of class Graf.
     */
    @Test
    public void testUkloniBrid() {
        System.out.println("ukloniBrid");
        int i = 0;
        int j = 0;
        Graf instance = new Graf();
        instance.ukloniBrid(i, j);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of ukloniVrh method, of class Graf.
     */
    @Test
    public void testUkloniVrh() {
        System.out.println("ukloniVrh");
        int idx = 0;
        Graf instance = new Graf();
        instance.ukloniVrh(idx);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of ispišiMatricu method, of class Graf.
     */
    @Test
    public void testIspišiMatricu() {
        System.out.println("ispi\u0161iMatricu");
        float[][] m = null;
        Graf.ispišiMatricu(m);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of dajVrhove method, of class Graf.
     */
    @Test
    public void testDajVrhove() {
        System.out.println("dajVrhove");
        Graf instance = new Graf();
        Integer[] expResult = null;
        Integer[] result = instance.dajVrhove();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sequentialMIS method, of class Graf.
     */
    @Test
    public void testSequentialMIS() {
        System.out.println("sequentialMIS");
        
    }

    /**
     * Test of parallelMIS1 method, of class Graf.
     */
    @Test
    public void testParallelMIS1() {
        System.out.println("parallelMIS1");
        Graf instance = new Graf();
        FutureTask<ArrayList<Integer>> expResult = null;
        FutureTask<ArrayList<Integer>> result = instance.parallelMIS1();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
