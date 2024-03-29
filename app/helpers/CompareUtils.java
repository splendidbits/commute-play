package helpers;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 09/07/2017 Splendid Bits.
 */
public class CompareUtils {

    /**
     * Check that two objects are identical, not including whitespace differences in strings, lists are
     * the same (.isEmpty() == null
     * and other small non-strict equality measures.
     *
     * @param object1 first object.
     * @param object2 second object.
     * @return boolean true if all objects are considered the same.
     */
    public static boolean isEquals(Object object1, Object object2) {
        if (object1 == null && object2 == null) {
            return true;
        }

        // Collections
        if (object1 instanceof Collection) {
            Collection collection1 = (Collection) object1;
            if (collection1.isEmpty() && object2 == null) {
                return true;
            }
        }

        if (object2 instanceof Collection) {
            Collection collection2 = (Collection) object2;
            if (collection2.isEmpty() && object1 == null) {
                return true;
            }
        }

        if (object1 instanceof Collection && object2 instanceof Collection) {
            if (((Collection) object1).isEmpty() && ((Collection) object2).isEmpty()) {
                return true;
            }

            for (Object object1Item : (Collection) object1) {
                if (!((Collection) object2).contains(object1Item)) {
                    return false;
                }
            }

            for (Object object2Item : (Collection) object2) {
                if (!((Collection) object1).contains(object2Item)) {
                    return false;
                }
            }
            return true;
        }

        // String
        if (object1 instanceof CharSequence) {
            CharSequence string1 = (CharSequence) object1;
            if (string1.length() == 0 && object2 == null) {
                return true;
            }
        }

        if (object2 instanceof CharSequence) {
            CharSequence string2 = (CharSequence) object2;
            if (string2.length() == 0 && object1 == null) {
                return true;
            }
        }

        // Date
        if ((object1 instanceof Date && object2 instanceof Date) ||
                (object1 instanceof Calendar && object2 instanceof Calendar)) {

            Date date1;
            Date date2;

            if (object1 instanceof Calendar) {
                date1 = ((Calendar) object1).getTime();
                date2 = ((Calendar) object1).getTime();
            } else {
                date1 = (Date) object1;
                date2 = (Date) object2;
            }

            if (DateUtils.isSameInstant(date1, date2)) {
                return true;
            }
        }

        if (object1 != null && object2 != null) {
            return object1.hashCode() == object2.hashCode();
        }

        return false;
    }
}
