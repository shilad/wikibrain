package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.lang.Language;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Yulun Li
 *
 * Parses a command line script and generates a .tsv file with the links to the dumps
 * of specified file type and languages.
 *
 */
public class DumpLinkGetter {

    protected static final String BASEURL_STRING = "http://dumps.wikimedia.org";

    private Language lang;
    private List<LinkMatcher> matchers;
    private EnumSet<LinkMatcher> hasMatchers;
    private String dumpDate;    // This is the date of the dump.

    public DumpLinkGetter(Language lang, List<LinkMatcher> matchers, String dumpDate) {
        this.lang = lang;
        this.matchers = matchers;
        this.dumpDate = dumpDate;
    }

    protected String getLanguageWikiUrl() {
        // langCode with dashes like "roa-tara" should be 'roa_tara' in dump links
        return BASEURL_STRING + "/" + lang.getLangCode().replace("-", "_") + "wiki/";
    }

    /**
     * Given the html of a page, return all links.
     *
     * @param html
     * @return
     */
    private List<String> getLinks(String html) {
        List<String> matches = new ArrayList<String>();
        Document doc = Jsoup.parse(html);
        Elements linkElements = doc.select("a[href]");
        for (Element linkElement: linkElements) {
            String link = linkElement.attr("href");
            matches.add(link);
        }
        return matches;
    }

    public List<String> getFileLinks() throws IOException {
        List<String> links = new ArrayList<String>();
        URL dumpPageUrl = new URL(getLanguageWikiUrl() + dumpDate + "/");
        Document doc = Jsoup.parse(IOUtils.toString(dumpPageUrl.openStream()));
        Elements linkElements = doc.select("ul").select("li.done").select("li.file").select("a[href]");
        for (Element linkElement : linkElements) {
            links.add(linkElement.attr("href"));
        }
        return links;
    }

    /**
     * Return all links of a particular language the fits one of the patterns.
     * @return
     */
    public HashMap<String, List<URL>> getDumpFiles() throws IOException {
        List<String> links = getFileLinks();
        HashMap<String, List<URL>> urlLinks = new HashMap<String, List<URL>>();
        for(LinkMatcher linkMatcher : matchers){
            List<String> results = linkMatcher.match(links);
            if (!results.isEmpty()) {
                List<URL> urls = new ArrayList<URL>();
                for (String url: results){
                    URL linkURL = new URL(BASEURL_STRING + url);
                    urls.add(linkURL);
                }
                urlLinks.put(linkMatcher.getName(), urls);
            }
        }
        return urlLinks;
    }
}