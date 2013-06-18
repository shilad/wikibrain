package org.wikapidia.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
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
 *
 */
public class DumpLinkGetter {

    private List<LinkMatcher> matchers;
    private Language lang;

    /**
     *
     * @param matchers
     * @param lang
     */
    public DumpLinkGetter(List<LinkMatcher> matchers, Language lang) {
        this.matchers = matchers;
        this.lang = lang;
    }

    /**
     * Given a particulat language, return the base url string of the dump of the language.
     *
     * @param lang
     * @return
     */
    private String validBaseURLString(Language lang) {
        String langCode = lang.getLangCode();
        // langCode with dashes like "roa-tara" should be 'roa_tara' in dump links
        String validLangCode = langCode.replace("-", "_");
        return BASEURL_STRING.replace("__LANG__", validLangCode);
    }

    /**
     * Return the html of the database dump index page.
     *
     * @return
     */
    protected String getDumpIndex() throws IOException {
        String indexPageAddress = validBaseURLString(lang);
        URL indexURL = new URL(indexPageAddress);
        return IOUtils.toString(indexURL.openStream());
    }

    /**
     * Given the html of an index page and a particular pattern, return all links in html that fits the pattern.
     *
     * @param html
     * @return
     */
    protected List<String> getMatchingPatterns(String html) {
        List<String> matches = new ArrayList<String>();
        Document doc = Jsoup.parse(html);
        Elements linkElements = doc.select("a[href]");
        for (Element linkElement: linkElements) {
            matches.add(linkElement.toString());
        }
//        for (Element linkElement : linkElements) {
//            String link = linkElement.attr("href");
//            if(link.matches(pattern.toString())){
//                try {
//                    URL linkURL = new URL(validBaseURLString(lang) + link);
//                    matches.add(linkURL);
//                } catch (MalformedURLException e) {
//                    e.printStackTrace();  //How to handle this?
//                    return null;
//                }
//            }
//        }
        return matches;
    }

    /**
     * Return all links of a particular language the fits one of the patterns.
     * @return
     */
    public List<URL> getDumpFiles() throws IOException {
        List<URL> allLinks = new ArrayList<URL>();
        for(String linkPattern : ){
            allLinks.addAll(
                    getMatchingPatterns(getDumpIndex(), Pattern.compile(linkPattern)));
        }
        return allLinks;
    }

    /**
     *
     *
     */
    public void main() {

    }

    public static final String BASEURL_STRING = "http://dumps.wikimedia.org/__LANG__wiki/latest/";

}
