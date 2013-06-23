package org.wikapidia.phrases.stanford;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.Fraction;
import org.wikapidia.utils.WpIOUtils;
import org.wikapidia.utils.WpStringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single dictionary entry corresponding to a line from a
 * dictionary.bz2 at http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/.
 *
 * Major components of an entry are:
 * - textual phrase
 * - concept (a wikipedia article)
 * - A variety of flags
 */
public class InputRecord implements Comparable<InputRecord> {
    private String text;
    private float fraction;
    private String article;
    private int numLinks;
    private String flags[];

    // Format: text + "\t" + probability + " " + url + " " + flags...
    private static final Pattern MATCH_ENTRY =
            Pattern.compile("([^\t]*)\t([0-9.e-]+) ([^ ]*)(| (.*))$");
    private String line;

    public InputRecord(String line) {
        Matcher m = MATCH_ENTRY.matcher(line);
        if (!m.matches()) {
            throw new IllegalArgumentException("invalid concepts entry: '" + line + "'");
        }
        this.text = m.group(1);
        this.fraction = Float.valueOf(m.group(2));
        this.article = m.group(3);
        this.flags = m.group(4).trim().split(" ");
        this.numLinks = getNumberEnglishLinks();

        // Strip newline
        if (!line.isEmpty() && line.endsWith("\n")) {
            line = line.substring(0, line.length() - 1);
        }
        this.line = line;
    }

    public String getText() {
        return text;
    }

    public float getFraction() {
        return fraction;
    }

    public String getArticle() {
        return article;
    }

    public String[] getFlags() {
        return flags;
    }

    public String toString() {
        return line;
    }

    public Fraction getFractionEnglishLinks() {
        for (String f : flags) {
            if (f.startsWith("W:")) {
                return Fraction.getFraction(f.substring(2));
            }
        }
        return null;
    }

    public int getNumberEnglishLinks() {
        Fraction f = getFractionEnglishLinks();
        return (f == null) ? 0 : f.getNumerator();
    }

    public boolean hasFlag(String flag) {
        return ArrayUtils.contains(flags, flag);
    }

    public String getNormalizedText() {
        return WpStringUtils.normalize(text);
    }
    @Override
    public int compareTo(InputRecord e) {
        return this.numLinks - e.numLinks;
    }
}
