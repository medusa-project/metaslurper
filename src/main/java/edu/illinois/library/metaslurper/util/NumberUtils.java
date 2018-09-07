package edu.illinois.library.metaslurper.util;

public final class NumberUtils {

    public static String percent(int numerator, int denominator) {
        if (denominator > 0) {
            return String.format("%.2f%%",
                    (numerator / (double) denominator) * 100);
        }
        return "?%";
    }

    private NumberUtils() {}

}
