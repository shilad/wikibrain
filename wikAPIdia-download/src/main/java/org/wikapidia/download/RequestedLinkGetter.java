package org.wikapidia.download;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;

import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 */
public class RequestedLinkGetter {

    private static final String DATE_FORMAT = "yyyyMMdd";

    private Language lang;
    private List<LinkMatcher> matchers;
    private String requestDate;    // This is the date requested by the user.

    private static final Logger LOG = Logger.getLogger(DumpLinkGetter.class.getName());


    public RequestedLinkGetter(Language lang, List<LinkMatcher> matchers, String requestDate) {
        this.lang = lang;
        this.matchers = matchers;
        this.requestDate = requestDate;
    }

    protected List<Date> getAllDates() throws IOException, ParseException {
        List<Date> availableDate = new ArrayList<Date>();
        URL langWikiPageUrl = new URL(DumpLinkGetter.BASEURL_STRING+ "/" + lang.getLangCode().replace("-", "_") + "wiki/");
        Document doc = Jsoup.parse(IOUtils.toString(langWikiPageUrl.openStream()));
        Elements availableDates = doc.select("tbody").select("td.n").select("a[href]");
        for (Element element : availableDates) {
            Matcher dateMatcher = Pattern.compile("(\\d{8})/").matcher(element.attr("href"));
            while (dateMatcher.find()) {
                availableDate.add(stringToDate(dateMatcher.group(1)));
            }
        }
        return availableDate;
    }




    protected List<String> availableDumpDatesSorted(List<Date> dateList) throws java.text.ParseException {
        List<String> dateListSorted = new ArrayList<String>();
        Collections.sort(dateList, new Comparator<Date>() {
            public int compare(Date date1, Date date2) {
                return date1.compareTo(date2);
            }
        });
        for (Date date : dateList) {
            if (!date.after(stringToDate(requestDate))) {
                dateListSorted.add(new SimpleDateFormat(DATE_FORMAT).format(date));
            }
        }
        return dateListSorted;
    }

    /**
     * Convert a String to a Date object.
     * @param dateString Date formatted in 'yyyyMMdd' as a string.
     * @return Date as java.util.Date object.
     * @throws java.text.ParseException
     */
    private Date stringToDate(String dateString) throws java.text.ParseException {
        return new SimpleDateFormat(DATE_FORMAT).parse(dateString);
    }

    protected HashMap<String, HashMap<String, List<URL>>> getDumps() throws ParseException, IOException, WikapidiaException {
        List<String> availableDates = availableDumpDatesSorted(getAllDates());
        HashMap<String, HashMap<String, List<URL>>> map = new HashMap<String, HashMap<String, List<URL>>>();
        List<LinkMatcher> unfoundMatchers = new ArrayList<LinkMatcher>(matchers);
        for (int i = availableDates.size() -1; i > -1; i--) {
            DumpLinkGetter dumpLinkGetter = new DumpLinkGetter(lang, unfoundMatchers, availableDates.get(i));
            HashMap<String, List<URL>> batchDumps = dumpLinkGetter.getDumpFiles();
            map.put(availableDates.get(i), batchDumps);
            for (int j = 0; j < unfoundMatchers.size(); j++) {
                LinkMatcher linkMatcher = unfoundMatchers.get(j);
                if (batchDumps.keySet().contains(linkMatcher.getName())) {
                    unfoundMatchers.remove(linkMatcher);
                    j--;
                }
            }
            if (unfoundMatchers.isEmpty()) {
                return map;
            }
            if (i == 0) {
                LOG.log(Level.WARNING, "Some matchers not found: " + unfoundMatchers);
            }
        }
        return map;
    }

    /**
     * Parse command line and generate .tsv file containing language code, name of file type and link.
     *
     */
    public static void main(String[] args) throws IOException, WikapidiaException, ParseException {

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
                        .withLongOpt("date")
                        .withDescription("Dumps are pulled from on or before this date. Default is today")
                        .create("d"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("RequestedLinkGetter", options);
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

        String getDumpByDate = cmd.hasOption("d") ? cmd.getOptionValue('d') : new SimpleDateFormat(DATE_FORMAT).format(new Date());
        String filePath = cmd.getOptionValue('o');
        File file = new File(filePath);

        List<String> result = new ArrayList<String>();
        for (Language language : languages) {
            RequestedLinkGetter requestedLinkGetter = new RequestedLinkGetter(language, linkMatchers, getDumpByDate);
            HashMap<String, HashMap<String, List<URL>>> urls = requestedLinkGetter.getDumps();
            for (String dumpDate : urls.keySet()) {
                for (String linkName : urls.get(dumpDate).keySet()) {
                    for (URL url : urls.get(dumpDate).get(linkName)) {
                        result.add(language.getLangCode() + " " + dumpDate + "\t" + linkName + "\t" + url);
                    }
                }
            }
        }
        FileUtils.writeLines(file, result, "\n");
    }
}
