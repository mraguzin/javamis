package misgui;

import java.awt.geom.Line2D;
import java.util.Objects;

/**
 * Apstrakcija Swingovog segmenta pravca radi podrške pomicanja i detekcije
 * jednakosti isključivo preko jednakosti krajnjih vrhova (krugova) segmenta.
 * 
 * @author mraguzin
 */
public class Segment extends Line2D.Float {
    private Krug krug0, krug1;
    
    public Segment(Krug krug0, Krug krug1) {
        postavi(krug0, krug1);     
    }
    
    /**
     * Postavlja segment između <b>krug0</b> i <b>krug1</b> uz obrezivanje uzduž
     * pravca t.d. segment i krajevi (krugovi) budu geometrijski disjunktni.
     * @param krug0
     * @param krug1 
     */
    private void postavi(Krug krug0, Krug krug1) {
        double x0 = krug0.dajX();
        double y0 = krug0.dajY();
        double x1 = krug1.dajX();
        double y1 = krug1.dajY();
        double radijus0 = krug0.dajRadijus();
        double radijus1 = krug1.dajRadijus();
        this.krug0 = krug0;
        this.krug1 = krug1;
        
        double dx = x1 - x0;
        double dy = y1 - y0;
        if (dx != 0) {
            double koeficijentSmjera = dy / dx;
            double alpha = Math.atan2(dy, dx);
            double x0t = Math.cos(alpha) * radijus0;
            double y0t = x0t * koeficijentSmjera;
            double udaljenostTocaka = Math.sqrt(dx*dx + dy*dy);
            x0 += x0t;
            y0 += y0t;
            
            double x1t = x0t / radijus0 * (udaljenostTocaka - radijus1 - radijus0);
            double y1t = x1t * koeficijentSmjera;
            x1 = x0 + x1t;
            y1 = y0 + y1t;
        }
        else {
            double sign = Math.signum(dy);
            y0 += radijus0 * sign;
            y1 -= radijus1 * sign;
        }
        
        super.x1 = (float)x0;
        super.x2 = (float)x1;
        super.y1 = (float)y0;
        super.y2 = (float)y1;     
    }
    
    public void pomakniKraj(Krug kraj, float x, float y) {
        Krug k = (Krug) kraj.clone();
        k.pomakni(x, y);
        if (kraj.equals(krug0))
            postavi(k, krug1);
        else
            postavi(krug0, k);
    }
    
    public Krug dajKrug1() {
        return krug0;
    }
    
    public Krug dajKrug2() {
        return krug1;
    }
    
    public void postaviKrug1(Krug k) {
        krug0 = k;
    }
    
    public void postaviKrug2(Krug k) {
        krug1 = k;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        
        if (o instanceof Segment) {
            Segment s = (Segment)o;
            return s.krug0.equals(krug0) && s.krug1.equals(krug1) ||
                    s.krug0.equals(krug1) && s.krug1.equals(krug0);
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.krug0);
        hash = 17 * hash + Objects.hashCode(this.krug1);
        return hash;
    }
}
