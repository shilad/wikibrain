package org.wikapidia.utils;

import java.util.regex.Pattern;

public class WpStringUtils {
    /**
     * Replaces consecutive non alpha-numeric characters with a space
     * Converts to lowercase
     * Removes whitespace
     */
    public static String normalize(String s) {
        return REPLACE_WEIRD.matcher(s).replaceAll(" ").toLowerCase().trim();
    }
    private static Pattern REPLACE_WEIRD = Pattern.compile("[^\\p{L}\\p{N}]+");
}
