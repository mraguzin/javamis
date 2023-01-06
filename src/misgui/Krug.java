package misgui;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

/**
 *
 * @author mraguzin
 */
public class Krug extends Ellipse2D.Float {
    private static final Color standardna = Color.GRAY;
    private static final Color posebna = Color.RED; // za čvorove u rješenju
    private static final Color aktivna = Color.YELLOW; // kada kliknemo na krug
    
    private Color boja;
    private float radijus;
    private float x, y;
    private int id;
    
    public Krug(int id, float x, float y, float radijus) {
        super(x - radijus, y - radijus, 2.0f * radijus, 2.0f * radijus);
        this.radijus = radijus;
        this.x = x;
        this.y = y;
        this.id = id;
    }
    
    public void resetiraj() {
        boja = standardna;
    }
    
    public void oboji() {
        boja = posebna;
    }
    
    public void aktiviraj() {
        boja = aktivna;
    }
    
    public float dajRadijus() {
        return radijus;
    }
    
    public float dajX() {
        return x;
    }
    
    public float dajY() {
        return y;
    }
    
    public int dajId() {
        return id;
    }
    
    public void postaviId(int id) {
        this.id = id;
    }
    
    @Override
    public boolean equals(Object k) {
        if (this == k)
            return true;
        
        if (k instanceof Krug) {
            Krug krug = (Krug)k;
            if (krug.id == id)
                return true;
        }
            
        return false;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
}
