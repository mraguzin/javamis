package parallelmis.helpers;

/**
 *
 * @author mraguzin
 */
public class SharedDouble {
    private double value;
    
    public SharedDouble(double value) {
        this.value = value;
    }
    
    public double get() {
        return value;
    }
    
    public void set(double value) {
        this.value = value;
    }
}
