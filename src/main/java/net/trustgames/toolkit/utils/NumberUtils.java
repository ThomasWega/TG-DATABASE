package net.trustgames.toolkit.utils;

public class NumberUtils {

    private NumberUtils() {}

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
