package org.wikibrain.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static long longHashCode(String s) {
       MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);  // should not happen
        }
        try {
            messageDigest.update(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);  // should not happen
        }
        byte[] bytes = messageDigest.digest();
        long h = 1125899906842597L; //prime
        for (byte b: bytes) {
            h = 31*h + b;
        }
        return h;
    }

    /**
     * This is much faster... TODO: look for and replace longHashCode calls.
     * @param s
     * @return
     */
    public static long longHashCode2(String s) {
        return MurmurHash.hash64(s);
    }
}
