package parallelmis;

/**
 *
 * @author mraguzin
 * @param <U>
 * @param <V>
 * @param <T>
 */
public class Trojka<U, V, T> {
    public U prvi;
    public V drugi;
    public T treći;
    
    @Override
    public String toString() {
        return "(" + prvi.toString() + ", " + drugi.toString() + ", " +
                treći.toString() + ")";
    }
    
}
