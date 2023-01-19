//package parallelmis.helpers;
//// neke pomoćne funkcije
//
//import java.util.ArrayList;
//
//
///**
// *
// * @author mraguzin
// */
//public class Pomoćne {
//    public static int zadnji(double A[], double x, int left, int right) {
//        if (left == right) {
//            return left;
//        }
//
//        int mid = (left + right) / 2;
//        if (A[mid] < x)
//            return zadnji(A, x, mid+1, right);
//        else if (A[mid] > x)
//            return zadnji(A, x, left, mid-1);
//        else if (mid == right || A[mid+1] != x)
//            return mid;
//        else
//            return zadnji(A, x, mid+1, right);
//    }
//
//    public static <T extends Comparable<T>> int zadnji(T A[], T x, int l, int r) {
//        if (l == r) {
//            if (A[l].compareTo(x) == 0)
//                return l;
//            else
//                return -1;
//        }
//
//        int mid = (l + r) / 2;
//        if (A[mid].compareTo(x) < 0)
//            return zadnji(A, x, mid+1, r);
//        else if (A[mid].compareTo(x) > 0)
//            return zadnji(A, x, l, mid-1);
//        else if (mid == r || A[mid+1].compareTo(x) != 0)
//            return mid;
//        else
//            return zadnji(A, x, mid+1, r);
//    }
//
//    public static Integer[] presjek(int A[], int B[]) {
//        int n = A.length;
//        var C = new ArrayList<Integer>();
//
//        int i = 0;
//        int j = 0;
//        while (i < n && j < n) {
//            if (A[i] < B[j])
//                i++;
//            else if (A[i] > B[j])
//                j++;
//            else {
//                C.add(A[i++]);
//                j++;
//            }
//        }
//
//        Integer[] tmp = {};
//        return C.toArray(tmp);
//    }
//}
