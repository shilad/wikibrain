package org.wikibrain.cookbook.phrases;

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Matt Lesicko
 * Date: 7/2/13
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class CheckHashCollisions {
    private static Long hash(String title){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(title.getBytes());
            byte[] bytes = messageDigest.digest();
            long h = 1125899906842597L;
            for (byte b : bytes){
                h=31*h+b;
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main (String[] args) throws Exception {
        String file = "./enwiki-20130604-all-titles-in-ns0";
        Set<Long> titleHashes = new HashSet<Long>();
        BufferedReader titles = new BufferedReader(new FileReader (file));
        String title = titles.readLine(); //Intentionally skipping the first line
        title = titles.readLine();
        int i=0, j=0;
        while(title!=null){
            Long hash = hash(title);
            assert(hash!=null);
            if (titleHashes.contains(hash)){
                //throw new Exception("HASHING COLLISION");
                j++;
            }
            titleHashes.add(hash);
            title=titles.readLine();
        }
        System.out.println(titleHashes.size()+" entries, "+j+" collisions");
    }
}
