package helpers;

/**
 * Object Comparison utilities.
 */
public class CompareUtils {

    public static boolean equalsNullSafe(Object object1, Object object2) {
        if (object1 == null && object2 == null) {
            return true;
        }

        if (object1 != null && object2 != null) {
            return object1.equals(object2);
        }

        return false;
    }
}
