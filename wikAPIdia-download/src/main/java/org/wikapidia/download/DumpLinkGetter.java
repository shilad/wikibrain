package org.wikapidia.download;

import java.io.IOException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikapidia.core.lang.Language;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

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
    private String dumpDate;    // This is the date of the dump.

    public DumpLinkGetter(Language lang, List<LinkMatcher> matchers, String dumpDate) {
        this.lang = lang;
        this.matchers = matchers;
        this.dumpDate = dumpDate;
    }

    /**
     * Get the URL of the index wiki page of a specified language.
     * @return
     */
    protected String getLanguageWikiUrl() {
        // langCode with dashes like "roa-tara" should be 'roa_tara' in dump links
        return BASEURL_STRING + "/" + lang.getLangCode().replace("-", "_") + "wiki/";
    }

    /**
     * Get file links that are marked "done" (plus MD5sum) on a dump page of the specified language and specified dumpDate
     * @return
     * @throws IOException
     */
    public List<String> getFileLinks() throws IOException {
        List<String> links = new ArrayList<String>();
        URL dumpPageUrl = new URL(getLanguageWikiUrl() + dumpDate + "/");
        Document doc = Jsoup.parse(IOUtils.toString(dumpPageUrl.openStream()));
        Elements linkElements = doc.select("ul").select("li.done").select("li.file").select("a[href]");
        linkElements.addAll(doc.select("p.checksum").select("a[href]"));
        for (Element linkElement : linkElements) {
            links.add(linkElement.attr("href"));
        }
        return links;
    }

    /**
     * Return all links of a particular language the fits one of the patterns
     * @return  hashmap with dump urls and names of dump type
     */
    public Multimap<LinkMatcher, DumpLinkInfo> getDumpFiles(List<String> links) throws IOException {
        Multimap<LinkMatcher, DumpLinkInfo> dumpLinks = HashMultimap.create();
        Map<String, String> md5s = getMd5Sums(links);
        for(LinkMatcher linkMatcher : matchers){
            List<String> results = linkMatcher.match(links);
            if (!results.isEmpty()) {
                for (String url : results){
                    URL linkURL = new URL(BASEURL_STRING + url);
                    DumpLinkInfo linkInfo = new DumpLinkInfo(lang, dumpDate, linkMatcher, linkURL);
                    linkInfo.setMd5(md5s.get(linkInfo.getDownloadName()));
                    dumpLinks.put(linkMatcher, linkInfo);
                }
            }
        }
        return dumpLinks;
    }

    /**
     * Get MD5 of the dump of the specified language and dumpDate.
     * Maps download name to MD5 sum.
     * @param links
     * @return
     * @throws IOException
     */
    protected Map<String, String> getMd5Sums(List<String> links) throws IOException {
        LinkMatcher md5Matcher = LinkMatcher.MD5;
        URL md5Url = new URL(BASEURL_STRING + md5Matcher.match(links).get(0));
        List<String> lines = IOUtils.readLines(md5Url.openStream(), "UTF-8");
        HashMap<String, String> md5s = new HashMap<String, String>();
        for (String line : lines) {
            String[] parsedInfo = line.split("\\W{2}");
            String md5 = parsedInfo[0];
            String fileName = parsedInfo[1];
            md5s.put(fileName, md5);
        }
        return md5s;
    }

    public static void main(String[] args) throws IOException {
        DumpLinkGetter testGetter = new DumpLinkGetter(Language.getByLangCode("en"), Arrays.asList(LinkMatcher.ARTICLES), "20130604");
//        System.out.println(testGetter.getMd5Sums(testGetter.getFileLinks()));
        System.out.println(testGetter.getDumpFiles(testGetter.getFileLinks()));

    }

}