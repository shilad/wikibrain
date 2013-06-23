package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.cli.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Parses a command line script and generates a .tsv file with the links to the dumps
 * of specified file type and languages.
 *
 */
public class DumpLinkGetter {

    private final String baseUrl;
    private List<LinkMatcher> matchers;
    private Language lang;

    private static final Logger LOG = Logger.getLogger(DumpLinkGetter.class.getName());

    /**
     *
     * @param matchers
     * @param lang
     */
    public DumpLinkGetter(String baseUrl, List<LinkMatcher> matchers, Language lang) {
        this.baseUrl = baseUrl;
        this.matchers = matchers;
        this.lang = lang;
    }

    /**
     * Given a particulat language, return the base url string of the dump of the language.
     *
     * @param lang
     * @return
     */
    private String getLanguageBaseUrl(Language lang) {
        // langCode with dashes like "roa-tara" should be 'roa_tara' in dump links
        return BASEURL_STRING.replace("__LANG__", lang.getLangCode().replace("-", "_"));
    }

    /**
     * Return the html of the database dump index page.
     *
     * @return
     */
    protected String getDumpIndex() throws IOException {
        URL indexURL = new URL(getLanguageBaseUrl(lang));
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
                    URL linkURL = new URL(getLanguageBaseUrl(lang) + url);
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
     * Parse command line and generate .tsv file containing language code, name of file type and link.
     *
     */
    public static void main(String[] args) throws IOException {

        Options options = new Options();

        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("languages")
                        .withValueSeparator(',')
                        .withDescription("List of languages, separated by a comma (e.g. 'en,de'). Default is all languages.")
                        .create("l"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("names")
                        .withDescription("Names of file types, separated by comma (e.g. 'articles,abstracts'). Default is everything.")
                        .create("n"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("output")
                        .withDescription("Path to output file.")
                        .create("o"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("baseurl")
                        .withDescription("Base url for dumps. Defaults to " + BASEURL_STRING)
                        .create("u"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLinkGetter", options);
            return;
        }

        List<LinkMatcher> linkMatchers = Arrays.asList(LinkMatcher.values());
        if (cmd.hasOption("n")) {
            linkMatchers = new ArrayList<LinkMatcher>();
            for (String name : cmd.getOptionValues("n")) {
                LinkMatcher matcher = LinkMatcher.getByName(name);
                if (matcher == null) {
                    System.err.println("Invalid matcher name: " + name
                            + "\nValid matcher names: \n" + LinkMatcher.getAllNames().toString());
                    System.exit(1);
                }
                linkMatchers.add(matcher);
            }
        }

        List<Language> languages = Arrays.asList(Language.LANGUAGES);
        if (cmd.hasOption("l")) {
            languages = new ArrayList<Language>();
            for (String langCode : cmd.getOptionValues("l")) {
                try {
                    languages.add(Language.getByLangCode(langCode));
                } catch (IllegalArgumentException e) {
                    String langs = "";
                    for (Language language : Language.LANGUAGES) {
                        langs += language.getLangCode() + ",";
                    }
                    System.err.println("Invalid language code: " + langCode
                            + "\nValid language codes: \n" + langs);
                    System.exit(1);
                }
            }
        }

        String baseUrl = cmd.hasOption("u") ? cmd.getOptionValue('u') : BASEURL_STRING;
        String filePath = cmd.getOptionValue('o');
        File file = new File(filePath);

        List<String> result = new ArrayList<String>();
        for (Language language : languages) {
            DumpLinkGetter dumpLinkGetter = new DumpLinkGetter(baseUrl, linkMatchers, language);
            HashMap<String, List<URL>> urls = dumpLinkGetter.getDumpFiles();
            for (String linkName : urls.keySet()) {
                for (URL url : urls.get(linkName)) {
                    result.add(language.getLangCode() + "\t" + linkName + "\t" + url);
                }
            }
        }
        FileUtils.writeLines(file, result, "\n");
    }

    protected static final String BASEURL_STRING = "http://dumps.wikimedia.org/__LANG__wiki/latest/";

}