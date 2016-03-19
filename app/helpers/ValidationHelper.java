package helpers;

/**
 * Some helper methods for validation.
 */
public class ValidationHelper {
    public static boolean isNumeric(String str){
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

}
