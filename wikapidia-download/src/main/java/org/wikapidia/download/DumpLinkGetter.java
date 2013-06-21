package org.wikapidia.download;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.lang.Language;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        // langCode with dashes like "roa-tara" should be 'roa_tara' in dump links
        return BASEURL_STRING.replace("__LANG__", lang.getLangCode().replace("-", "_"));
    }

    /**
     * Return the html of the database dump index page.
     *
     * @return
     */
    protected String getDumpIndex() throws IOException {
        URL indexURL = new URL(validBaseURLString(lang));
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
    public HashMap<String, List<URL>> getDumpFiles() throws IOException {
        List<String> links = getLinks(getDumpIndex());
        HashMap<String, List<URL>> urlLinks = new HashMap<String, List<URL>>();
        try{
            for(LinkMatcher linkMatcher : matchers){
                List<String> results = linkMatcher.match(links);
                List<URL> urls = new ArrayList<URL>();
                for (String url: results){
                    URL linkURL = new URL(validBaseURLString(lang) + url);
                    urls.add(linkURL);
                }
                urlLinks.put(linkMatcher.getName(), urls);
            }
        } catch(MalformedURLException e){
            LOG.log(Level.WARNING, "string cannot form URL", e);
        }
        return urlLinks;
    }

    /**
     *
     *
     */
    public static void main(List<Language> languages, List<String> names, String filePath) throws IOException {

//        Options options = new Options();
//
//        options.addOption("l", true, "language");
//        options.addOption("n", true, "dump name");
//        options.addOption("o", true, "save to file path");
//
//        CommandLineParser parser = new PosixParser();
//        CommandLine cmd;
//        try {
//            cmd = parser.parse(options, args);
//        } catch (ParseException e) {
//            System.err.println( "Invalid option usage: " + e.getMessage());
//            new HelpFormatter().printHelp("DumpLinkGetter", options);
//            return;
//        }
//        List<String> names = cmd.hasOption("n") ? Arrays.asList(cmd.getOptionValues("n")) : Arrays.asList(new String[]{"articles"});
//        List<Language> languages = cmd.hasOption("l") ? Arrays.asList(cmd.getOptionValues("l")) : Arrays.asList(new Language[]{Language.getById(1)});
//        String filePath = cmd.hasOption("o") ? cmd.getOptionValue('o') : "~/";

        FileWriter writer = new FileWriter(filePath);

        for (Language language : languages) {
            List<LinkMatcher> linkMatchers = new ArrayList<LinkMatcher>();
            for (String name : names) {
                linkMatchers.add(LinkMatcher.valueOf(name));
            }
            DumpLinkGetter dumpLinkGetter = new DumpLinkGetter(linkMatchers, language);
            HashMap<String, List<URL>> urls = dumpLinkGetter.getDumpFiles();
            for (String linkName : urls.keySet()) {
                for (URL url : urls.get(linkName)) {
                    writer.append(language.getLangCode() + "\t" + linkName + "\t" + url + "\n");
                }
            }
        }
    }

    protected static final String BASEURL_STRING = "http://dumps.wikimedia.org/__LANG__wiki/latest/";

}
