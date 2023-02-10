package misgui;

import java.awt.Color;
import java.awt.geom.Ellipse2D;

/**
 *
 * @author mraguzin
 */
public class Krug extends Ellipse2D.Float {
    private static final Color STANDARDNA = Color.GRAY;
    private static final Color POSEBNA = Color.RED; // za čvorove u rješenju
    private static final Color AKTIVNA = Color.YELLOW; // kada kliknemo na krug
    
    private Color boja;
    private float radijus;
    private float x, y; // centar
    
    public Krug(float x, float y, float radijus) {
        super(x - radijus, y - radijus, 2.0f * radijus, 2.0f * radijus);
        this.boja = STANDARDNA;
        this.radijus = radijus;
        this.x = x;
        this.y = y;
    }
    
    public void pomakni(float noviX, float noviY) {
        super.x = noviX - radijus;
        super.y = noviY - radijus;
        this.x = noviX;
        this.y = noviY;
    }
    
    public Color dajBoju() {
        return boja;
    }
    
    public void resetiraj() {
        boja = STANDARDNA;
    }
    
    public void oboji() {
        boja = POSEBNA;
    }
    
    public void aktiviraj() {
        boja = AKTIVNA;
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
    
    @Override
    public boolean equals(Object k) {
        if (this == k)
            return true;

        if (k instanceof Krug) {
            Krug krug = (Krug)k;
            return krug.x == x && krug.y == y && krug.radijus == radijus;
        }
            
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + java.lang.Float.floatToIntBits(this.radijus);
        hash = 29 * hash + java.lang.Float.floatToIntBits(this.x);
        hash = 29 * hash + java.lang.Float.floatToIntBits(this.y);
        return hash;
    }
}
