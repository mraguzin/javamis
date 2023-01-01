package parallelmis.helpers;
// neke pomoćne funkcije

/**
 *
 * @author mraguzin
 */
public class Pomoćne {
    public static int zadnji(double A[], double x, int l, int r) {
        if (l == r) {
            if (A[l] == x)
                return l;
            else
                return -1;
        }
        
        int mid = (l + r) / 2;
        if (A[mid] < x)
            return zadnji(A, x, mid+1, r);
        else if (A[mid] > x)
            return zadnji(A, x, l, mid-1);
        else if (mid == r || A[mid+1] != x)
            return mid;
        else
            return zadnji(A, x, mid+1, r);
    }
    
    public static <T extends Comparable<T>> int zadnji(T A[], T x, int l, int r) {
        if (l == r) {
            if (A[l].compareTo(x) == 0)
                return l;
            else
                return -1;
        }
        
        int mid = (l + r) / 2;
        if (A[mid].compareTo(x) < 0)
            return zadnji(A, x, mid+1, r);
        else if (A[mid].compareTo(x) > 0)
            return zadnji(A, x, l, mid-1);
        else if (mid == r || A[mid+1].compareTo(x) != 0)
            return mid;
        else
            return zadnji(A, x, mid+1, r);
    }
}
