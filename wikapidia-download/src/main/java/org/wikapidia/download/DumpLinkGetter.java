package org.wikapidia.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikapidia.core.lang.Language;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This c
 */
public class DumpLinkGetter {
    public static final String PATTERN_PREFIX = "http://dumps.wikimedia.org/__LANG__wiki/latest/";
    public static final String PATTERN_ARTICLES = ".*wiki-latest-pages-articles[0-9]+.xml-.+.bz2";
    public static final String PATTERN_PAGELINKS = "http://dumps.wikimedia.org/__LANG__wiki/latest/__LANG__wiki-latest-pagelinks.sql.gz";
    public static final String PATTERN_PAGELINKSQL = ".*wiki-latest-pagelinks.sql.gz";

    private List<String> patterns;

    public DumpLinkGetter(){}

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
    protected List<Pattern> getLanguageSpecificPatterns(Language language) {
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
     *
     * @param html
     * @param pattern
     * @return
     */
    protected List<URL> getMatchingPatterns(String html, Pattern pattern) {
        List<URL> matches = new ArrayList<URL>();
        Document doc = Jsoup.parse(html);
        Elements linkElements = doc.select("a[href]");
        for (Element linkElement : linkElements) {
            String link = linkElement.attr("href");
            if(link.matches(pattern.toString())){
                try {
                    URL linkURL = new URL("http://dumps.wikimedia.org/enwiki/latest/" + link);
                    matches.add(linkURL);
                    System.out.println(link);
                } catch (MalformedURLException e) {
                    e.printStackTrace();  //How to handle this?
                    return null;
                }
            }
        }
        return matches;
    }

    /**
     * Given a language, return the html of the database dump index page.
     * @param lang
     * @return
     */
    protected String getDumpIndex(Language lang) {
        try {
            this.patterns = Arrays.asList(PATTERN_PREFIX);
            Pattern indexPattern = this.getLanguageSpecificPatterns(lang).get(0);
            URL indexURL = new URL(indexPattern.toString());
            BufferedReader pageReader = new BufferedReader(new InputStreamReader(indexURL.openStream()));
            StringBuffer stringBuffer = new StringBuffer();
            String inputLine;
            while((inputLine = pageReader.readLine()) != null) {
                stringBuffer.append(inputLine);
            }
            return stringBuffer.toString();
//            return Integer.toString(stringBuffer.toString().length());
        } catch (MalformedURLException e) {
            e.printStackTrace();  //How to handle this?
            return null;
        } catch (IOException e) {
            e.printStackTrace();  //How to handle this?
            return null;
        }
    }

    /**
     * @param lang
     * @return
     */
    public List<URL> getDumpFiles(Language lang) {
        return null;
    }
}
