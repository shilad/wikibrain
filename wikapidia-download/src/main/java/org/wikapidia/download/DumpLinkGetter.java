package org.wikapidia.download;

import org.wikapidia.core.lang.Language;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class DumpLinkGetter {
    public static final String PATTERN_ARTICLES = "http://dumps.wikimedia.org/__LANG__wiki/latest/__LANG__wiki-latest-pages-articles[0-9]+.xml-[a-z0-9]+.bz2";
                                                 //http://dumps.wikimedia.org/  en    wiki/latest/  en    wiki-latest-pages-articles1     .xml-p000000010p000010000.bz2
                                                 //http://dumps.wikimedia.org/__LANG__wiki/latest/__LANG__wiki-latest-pages-articles[0-9]+.xml-*                   .bz2
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
    public List<Pattern> getLanguageSpecificPatterns(Language language) {
        String langCode = language.getLangCode();
        // langCode with dashes like "roa-tara" should be 'roa_tara' in dump links
        String validLangCode = langCode.replace("-", "_");
        List<Pattern> linkREs = new ArrayList<Pattern>();
        for(String linkPattern : patterns){
            linkREs.add(Pattern.compile(linkPattern.replace("__LANG__", validLangCode)));
        }
        return linkREs;
    }

    /**
     * @param lang
     * @return
     */
    public List<URL> getDumpFiles(Language lang) {
        return null;
    }
}
