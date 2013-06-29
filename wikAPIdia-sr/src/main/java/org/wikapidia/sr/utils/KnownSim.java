package org.wikapidia.sr.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.wikapidia.core.lang.Language;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A single human labeled entry from the gold standard dataset.
 *
 * If the Wikipedia ids wpid1 and wpid2 are not -1, phrase1 and phrase2 contain
 * Wikipedia article titles (not random phrases), and the titles have been mapped
 * to Wikipedia ids.
 */
public class KnownSim {
    public String phrase1;
    public String phrase2;
    public int wpId1 = -1;
    public int wpId2 = -1;
    public Language language;
    public double similarity;

    public KnownSim(String phrase1, String phrase2, double similarity) {
        this.phrase1 = phrase1;
        this.phrase2 = phrase2;
        this.similarity = similarity;
    }

    public KnownSim(String phrase1, String phrase2, int wpId1, int wpId2, double similarity) {
        this.wpId1 = wpId1;
        this.wpId2 = wpId2;
        this.phrase1 = phrase1;
        this.phrase2 = phrase2;
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return "KnownSim{" +
                "phrase1='" + phrase1 + '\'' +
                ", phrase2='" + phrase2 + '\'' +
                ", similarity=" + similarity +
                '}';
    }

    /**
     * Swaps phrase1 and phrase2 50% of the time
     */
    public void maybeSwap() {
        if (Math.random() > 0.5) {
            String tp = phrase1;
            phrase1 = phrase2;
            phrase2 = tp;
            int tid = wpId1;
            wpId1 = wpId2;
            wpId2 = tid;
        }
    }

    public static final Logger LOG = Logger.getLogger(KnownSim.class.getName());

    public static List<KnownSim> read(File path) throws IOException {
        List<KnownSim> result = new ArrayList<KnownSim>();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String delim = "\t";
        if (path.getName().toLowerCase().endsWith("csv")) {
            delim = ",";
        }
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            String tokens[] = line.split(delim);
            if (tokens.length == 3) {
                result.add(new KnownSim(
                        tokens[0],
                        tokens[1],
                        Double.valueOf(tokens[2])
                ));
            } else {
                LOG.info("invalid line in gold standard file " + path + ": " +
                        "'" + StringEscapeUtils.escapeJava(line) + "'");
            }
        }
        return result;
    }
}
