package org.wikibrain.core.nlp;

import org.wikibrain.core.lang.Language;

import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class DictionaryBenchmarker {
    public static void main(String args[]) throws IOException {
        for (int i = 0; i < 10; i++) {
            Dictionary dictionary = new Dictionary(Language.SIMPLE, Dictionary.WordStorage.ON_DISK);
            long t0 = System.currentTimeMillis();
            dictionary.countNormalizedFile(new File(args[0]));
            long t1 = System.currentTimeMillis();
            System.err.println("counting took " + (t1 - t0) + " millis");
            File f = File.createTempFile("dictionary", "txt");
            f.delete();
            f.deleteOnExit();
            dictionary.write(f);
            f.delete();
            long t2 = System.currentTimeMillis();
            System.err.println("writing took " + (t2 - t1) + " millis");
        }
    }
}
