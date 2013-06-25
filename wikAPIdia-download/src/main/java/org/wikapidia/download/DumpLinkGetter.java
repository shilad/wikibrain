package org.wikapidia.download;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import edu.emory.mathcs.backport.java.util.Arrays;
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
import java.text.*;
import java.util.ArrayList;
import java.util.Date;
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
    private String dumpDate;    // This is the date of the dump.

    private static final Logger LOG = Logger.getLogger(DumpLinkGetter.class.getName());


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

     /**
     * Convert a string formatted in 'yyyyMMdd' to java.util.Date.
     * @param dateString
     * @return
     * @throws java.text.ParseException
     */
    private Date stringToDate(String dateString) throws java.text.ParseException {
        Date date = new SimpleDateFormat("yyyyMMdd").parse(dateString);
        return date;
    }

    public List<String> getFileLinks() throws IOException {
        try {
            List<String> links = new ArrayList<String>();
            URL dumpPageUrl = new URL(getLanguageWikiUrl() + dumpDate + "/");
            Document doc = Jsoup.parse(IOUtils.toString(dumpPageUrl.openStream()));
            Elements linkElements = doc.select("ul").select("li.done").select("li.file").select("a[href]");
            for (Element linkElement : linkElements) {
                links.add(linkElement.attr("href"));
            };
            return links;
        } catch (MalformedURLException e) {
            // TODO
            e.printStackTrace();
        }
        return null;
    }



//    public HashMap<String, List<URL>> getDoneDumps() throws IOException {

        /**
     * Return all links of a particular language the fits one of the patterns.
     * @return
     */
    public HashMap<String, List<URL>> getDumpFiles() throws IOException {
        List<String> links = getFileLinks();
        HashMap<String, List<URL>> urlLinks = new HashMap<String, List<URL>>();
        try{
            for(LinkMatcher linkMatcher : matchers){
                List<String> results = linkMatcher.match(links);
                List<URL> urls = new ArrayList<URL>();
                for (String url: results){
                    URL linkURL = new URL(BASEURL_STRING + url);
                    urls.add(linkURL);
                }
                urlLinks.put(linkMatcher.getName(), urls);
            }
        } catch(MalformedURLException e){
            LOG.log(Level.WARNING, "string cannot form URL", e);
        }
        return urlLinks;
    }







//
//    protected String getDumpIndexDate(String date) throws IOException {
//        URL tryIndexURL = new URL(BASEURL_STRING.replace("__LANG__", lang.getLangCode().replace("-", "_")) + date + "/");
//        Document doc = Jsoup.parse(IOUtils.toString(tryIndexURL.openStream()));
//        String status = doc.select("p.status").select("span").text();
//        return status.equals("Dump complete") ? date : "latest";
//    }
//
//    /**
//     * Return the html of the database dump index page.
//     *
//     * @return
//     */
//    protected String getDumpIndex(String date) throws IOException {
//        URL indexURL = new URL(getLanguageBaseUrl(lang) + date + "/");
//        return IOUtils.toString(indexURL.openStream());
//    }
//
//    /**
//     * Return the html of the database dump index page.
//     *
//     * @return
//     */
//    protected String getDumpIndex() throws IOException {
//        URL indexURL = new URL(getLanguageBaseUrl(lang));
//        return IOUtils.toString(indexURL.openStream());
//    }
//
//    /**
//     * Given the html of an index page, return all links.
//     *
//     * @param html
//     * @return
//     */
//    protected List<String> getLinks(String html) {
//        List<String> matches = new ArrayList<String>();
//        Document doc = Jsoup.parse(html);
//        Elements linkElements = doc.select("a[href]");
//        for (Element linkElement: linkElements) {
//            String link = linkElement.attr("href");
//            matches.add(link);
//        }
//        return matches;
//    }
//

//
//    /**
//     * Return all links of a particular language the fits one of the patterns.
//     * @return
//     */
//    public HashMap<String, List<URL>> getDumpFiles(String date) throws IOException {
//        List<String> links = getLinks(getDumpIndex(date));
//        HashMap<String, List<URL>> urlLinks = new HashMap<String, List<URL>>();
//        try{
//            for(LinkMatcher linkMatcher : matchers){
//                List<String> results = linkMatcher.match(links);
//                List<URL> urls = new ArrayList<URL>();
//                for (String url: results){
//                    URL linkURL = new URL(getLanguageBaseUrl(lang) + url);
//                    urls.add(linkURL);
//                }
//                urlLinks.put(linkMatcher.getName(), urls);
//            }
//        } catch(MalformedURLException e){
//            LOG.log(Level.WARNING, "string cannot form URL", e);
//        }
//        return urlLinks;
//    }
//
//    /**
//     * Parse command line and generate .tsv file containing language code, name of file type and link.
//     *
//     */
//    public static void main(String[] args) throws IOException {
//
//        Options options = new Options();
//
//        options.addOption(
//                new DefaultOptionBuilder()
//                        .hasArgs()
//                        .withLongOpt("languages")
//                        .withValueSeparator(',')
//                        .withDescription("List of languages, separated by a comma (e.g. 'en,de'). Default is all languages.")
//                        .create("l"));
//        options.addOption(
//                new DefaultOptionBuilder()
//                        .hasArgs()
//                        .withValueSeparator(',')
//                        .withLongOpt("names")
//                        .withDescription("Names of file types, separated by comma (e.g. 'articles,abstracts'). Default is everything.")
//                        .create("n"));
//        options.addOption(
//                new DefaultOptionBuilder()
//                        .hasArg()
//                        .isRequired()
//                        .withLongOpt("output")
//                        .withDescription("Path to output file.")
//                        .create("o"));
//        options.addOption(
//                new DefaultOptionBuilder()
//                        .hasArg()
//                        .withLongOpt("baseurl")
//                        .withDescription("Base url for dumps. Defaults to " + BASEURL_STRING)
//                        .create("u"));
//
//        CommandLineParser parser = new PosixParser();
//        CommandLine cmd;
//
//        try {
//            cmd = parser.parse(options, args);
//        } catch (ParseException e) {
//            System.err.println( "Invalid option usage: " + e.getMessage());
//            new HelpFormatter().printHelp("DumpLinkGetter", options);
//            return;
//        }
//
//        List<LinkMatcher> linkMatchers = Arrays.asList(LinkMatcher.values());
//        if (cmd.hasOption("n")) {
//            linkMatchers = new ArrayList<LinkMatcher>();
//            for (String name : cmd.getOptionValues("n")) {
//                LinkMatcher matcher = LinkMatcher.getByName(name);
//                if (matcher == null) {
//                    System.err.println("Invalid matcher name: " + name
//                            + "\nValid matcher names: \n" + LinkMatcher.getAllNames().toString());
//                    System.exit(1);
//                }
//                linkMatchers.add(matcher);
//            }
//        }
//
//        List<Language> languages = Arrays.asList(Language.LANGUAGES);
//        if (cmd.hasOption("l")) {
//            languages = new ArrayList<Language>();
//            for (String langCode : cmd.getOptionValues("l")) {
//                try {
//                    languages.add(Language.getByLangCode(langCode));
//                } catch (IllegalArgumentException e) {
//                    String langs = "";
//                    for (Language language : Language.LANGUAGES) {
//                        langs += language.getLangCode() + ",";
//                    }
//                    System.err.println("Invalid language code: " + langCode
//                            + "\nValid language codes: \n" + langs);
//                    System.exit(1);
//                }
//            }
//        }
//
//        String baseUrl = cmd.hasOption("u") ? cmd.getOptionValue('u') : BASEURL_STRING;
//        String filePath = cmd.getOptionValue('o');
//        File file = new File(filePath);
//
//        List<String> result = new ArrayList<String>();
//        for (Language language : languages) {
//            DumpLinkGetter dumpLinkGetter = new DumpLinkGetter(baseUrl, linkMatchers, language);
//            HashMap<String, List<URL>> urls = dumpLinkGetter.getDumpFiles();
//            for (String linkName : urls.keySet()) {
//                for (URL url : urls.get(linkName)) {
//                    result.add(language.getLangCode() + "\t" + linkName + "\t" + url);
//                }
//            }
//        }
//        FileUtils.writeLines(file, result, "\n");
//    }

}