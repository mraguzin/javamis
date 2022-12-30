package parallelmis;

/**
 *
 * @author mraguzin
 * @param <U>
 * @param <V>
 */
public class Par <U, V> {
        public U x;
        public V y;
        
        Par(U x, V y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }