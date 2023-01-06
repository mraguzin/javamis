package misgui;

import java.awt.geom.Line2D;

/**
 *
 * @author mraguzin
 */
public class Segment extends Line2D.Float {
    private int id0, id1;
    
    public Segment(Krug krug0, Krug krug1) {
        double x0 = krug0.dajX();
        double y0 = krug0.dajY();
        double x1 = krug1.dajX();
        double y1 = krug1.dajY();
        double R0 = krug0.dajRadijus();
        double R1 = krug1.dajRadijus();
        id0 = krug0.dajId();
        id1 = krug1.dajId();
        
        double dx = x1 - x0;
        double dy = y1 - y0;
        if (dx != 0) {
            double k = dy / dx;
            double alpha = Math.atan2(dy, dx);
            double x0t = Math.cos(alpha) * R0;
            double y0t = x0t * k;
            double l = Math.sqrt(dx*dx + dy*dy);
            x0 += x0t;
            y0 += y0t;
            
            double x1t = x0t / R0 * (l - R1 - R0);
            double y1t = x1t * k;
            x1 = x0 + x1t;
            y1 = y0 + y1t;
        }
        else {
            double sign = Math.signum(dy);
            y0 += R0 * sign;
            y1 -= R1 * sign;
        }
        
        super.x1 = (float)x0;
        super.x2 = (float)x1;
        super.y1 = (float)y0;
        super.y2 = (float)y1;        
    }
    
    public int dajKrug1() {
        return id0;
    }
    
    public int dajKrug2() {
        return id1;
    }
    
    public void postaviKrug1(int id) {
        id0 = id;
    }
    
    public void postaviKrug2(int id) {
        id1 = id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        
        if (o instanceof Segment) {
            Segment s = (Segment) o;
            if (s.id0 == id0 && s.id1 == id1 || s.id0 == id1 && s.id1 == id0)
                return true;
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.id0;
        hash = 59 * hash + this.id1;
        return hash;
    }
}
