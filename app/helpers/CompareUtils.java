package helpers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Date;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 09/07/2017 Splendid Bits.
 */
public class CompareUtils {

    /**
     * Check that two objects are identical, not including whitespace differences in strings,
     * and other small non-strict equality measures.
     *
     * @param objects a set of more than one objects to compare.
     * @return boolean true if all objects are considered the same.
     */
    public static boolean areAllEquals(@Nonnull Object... objects) {
        for (int i = 0; i < objects.length; i++) {

            if (i != 0) {
                if (objects[i] == null && objects[i-1] == null) {
                    continue;
                }

                // The object is not the same type as the previous object.
                if (objects[i] != null &&
                        !objects[i].getClass().getTypeName().equals(objects[i-1].getClass().getTypeName())) {
                    return false;
                }

                // String
                if (objects[i] instanceof CharSequence) {
                    if (!StringUtils.equalsAnyIgnoreCase((CharSequence) objects[i], (CharSequence) objects[i-1])) {
                        return false;
                    }

                // List
                } else if (objects[i] instanceof Collection) {
                    Collection collection1 = (Collection) objects[i];
                    Collection collection2 = (Collection) objects[i-1];

                    // Allow null == empty
                    if ((collection1 == null && collection2 != null && collection2.isEmpty()) ||
                            (collection1 != null && collection1.isEmpty() && collection2 == null)) {
                        continue;
                    }

                    if (!collection1.containsAll(collection2) || !collection2.containsAll(collection1)) {
                        return false;
                    }

                // Date
                } else if (objects[i] instanceof Date) {
                    Date date1 = (Date) objects[i];
                    Date date2 = (Date) objects[i - 1];

                    if (!DateUtils.isSameInstant(date1, date2)) {
                        return false;
                    }

                // All other types for now. Add each specialty type as needed.
                } else if (objects[i] != null && objects[i-1] != null &&
                        objects[i].hashCode() != objects[i-1].hashCode()) {
                    return false;

                // Finally, if one object is null and the other isn't
                } else if ((objects[i] == null && objects[i-1] != null) ||
                        (objects[i] != null && objects[i-1] == null)) {
                    return false;
                }
            }
        }
        return true;
    }
}
