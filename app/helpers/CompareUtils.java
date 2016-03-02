package helpers;

/**
 * Object Comparison utilities.
 */
public class CompareUtils {

    /**
     * Null-safe comparison check.
     *
     * @param object1 object one.
     * @param object2 object two.
     * @return true or false.
     */
    public static boolean equalsNullSafe(Object object1, Object object2) {
        if (object1 == null && object2 == null) {
            return true;
        }

        if (object1 != null && object2 != null) {
            return object1.equals(object2);
        }

        return false;
    }

    /**
     * Null-safe String null or empty check.
     *
     * @param string string to check.
     * @return true or false.
     */
//    public static boolean isEmptyNullSafe(String string) {
//        return (string == null || string.isEmpty());
//    }

    /**
     * Null-safe String null or empty check.
     *
     * @param string string to check.
     * @return true or false.
     */
    public static boolean isEmptyNullSafe(String... string) {
        for (String currentString : string) {
            if (currentString != null && !currentString.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}