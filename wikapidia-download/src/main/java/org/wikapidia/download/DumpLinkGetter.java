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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class DumpLinkGetter {

    private List<LinkMatcher> matchers;
    private Language lang;

    private static final Logger LOG = Logger.getLogger(DumpLinkGetter.class.getName());

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
     * Given the html of an index page and a particular pattern, return all links.
     *
     * @param html
     * @return
     */
    protected List<String> getLinks(String html) {
        List<String> matches = new ArrayList<String>();
        Document doc = Jsoup.parse(html);
        Elements linkElements = doc.select("a[href]");
        for (Element linkElement: linkElements) {
            String link = linkElement.attr("href");
            matches.add(link);
        }
        return matches;
    }

    /**
     * Return all links of a particular language the fits one of the patterns.
     * @return
     */
    public List<URL> getDumpFiles() throws IOException {
        List<String> links = getLinks(getDumpIndex());
        List<String> result = new ArrayList<String>();
        List<URL> urlLinks = new ArrayList<URL>();
        for (LinkMatcher linkMatcher : matchers){
            result.addAll(
                    linkMatcher.match(links));
        }
        for (String url : result) {
            try {
                URL linkURL = new URL(/*validBaseURLString(lang) + */url);
                urlLinks.add(linkURL);
            } catch (MalformedURLException e) {
                LOG.log(Level.WARNING, "article iterator construction failed", e); // TODO: WARNING OR SEVERE
            }
        }
        return urlLinks;
    }

    /**
     *
     *
     */
    public void main() {

    }

    public static final String BASEURL_STRING = "http://dumps.wikimedia.org/__LANG__wiki/latest/";

}
