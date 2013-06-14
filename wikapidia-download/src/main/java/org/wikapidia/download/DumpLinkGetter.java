package org.wikapidia.download;

import org.wikapidia.core.lang.Language;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class DumpLinkGetter {
    private final List<String> patterns;

    /**
     * Example pattern to match enwiki-latest-pages-articles1.xml-p000000010p000010000.bz2 and friends
     *
     * "LANGwiki-latest-pages-articles[0-9]+.xml.*.bz2"
     *
     *
     * @param patterns
     */
    public DumpLinkGetter(List<String> patterns) {
        this.patterns = patterns;
    }

    /**
     * Given a particular language, construct a set of language-specific regular expressions.
     *
     * @param language
     * @return
     */
    private List<Pattern> getLanguageSpecificPatterns(Language language) {
        return null;
    }

    /**
     * @param lang
     * @return
     */
    public List<URL> getDumpFiles(Language lang) {
        return null;
    }
}
