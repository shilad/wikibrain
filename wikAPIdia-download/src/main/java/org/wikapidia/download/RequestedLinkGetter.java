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
 * @author Ari Weiland
 * @author Yulun Li
 *
 * Get URLs of the dump file links with specified language, type of dump file and the date before which the dumps
 * are pulled.
 *
 */
    public class RequestedLinkGetter {

    private static final String DATE_FORMAT = "yyyyMMdd";

    private Language lang;
    private List<LinkMatcher> matchers;
    private Date requestDate;    // This is the date requested by the user.

    private static final Logger LOG = Logger.getLogger(DumpLinkGetter.class.getName());

    public RequestedLinkGetter(Language lang, List<LinkMatcher> matchers, Date requestDate) {
        this.lang = lang;
        this.matchers = matchers;
        this.requestDate = requestDate;
    }

    /**
     * Return all dates on the dump index page of a particular language.
     * @return list of Date objects.
     * @throws IOException
     * @throws ParseException
     */
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

    /**
     * Return a sorted list of dump dates before the date requested.
     * @param dateList list of Date object
     * @return list of dates as String.
     */
    protected List<String> availableDumpDatesSorted(List<Date> dateList) throws WikapidiaException {
        List<String> dateListSorted = new ArrayList<String>();
        Collections.sort(dateList, new Comparator<Date>() {
            public int compare(Date date1, Date date2) {
                return date1.compareTo(date2);
            }
        });
        for (Date date : dateList) {
            if (!date.after(requestDate)) {
                dateListSorted.add(new SimpleDateFormat(DATE_FORMAT).format(date));
            }
        }
        if (dateListSorted.isEmpty()) {
            throw new WikapidiaException("No dumps for " + lang.getLangCode() + " found before " + new SimpleDateFormat(DATE_FORMAT).format(requestDate));
        }
        return dateListSorted;
    }

    /**
     * Convert a String to a Date object.
     * @param dateString Date formatted in 'yyyyMMdd' as a string.
     * @return Date as java.util.Date object.
     * @throws java.text.ParseException
     */
    private static Date stringToDate(String dateString) throws java.text.ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
        dateFormatter.setLenient(false);
        return dateFormatter.parse(dateString);
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
     * Parse command line and generate .tsv file containing language code, date of dump, name of file type and link url.
     * @param args command line prompt
     * @throws IOException
     * @throws WikapidiaException
     * @throws ParseException
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
                        langs += "," + language.getLangCode();
                    }
                    langs = langs.substring(1);
                    System.err.println("Invalid language code: " + langCode
                            + "\nValid language codes: \n" + langs);
                    System.exit(1);
                }
            }
        }

        Date getDumpByDate = new Date();
        if (cmd.hasOption("d")) {
            try {
                getDumpByDate = stringToDate(cmd.getOptionValue("d"));
            } catch (java.text.ParseException e) {
                System.err.println("Invalid date: " + cmd.getOptionValue("d")
                        + "\nValid date format: \n" + "yyyyMMdd");
                System.exit(1);
            }
        }

        String filePath = cmd.getOptionValue('o');

        List<String> result = new ArrayList<String>();
        for (Language language : languages) {
            RequestedLinkGetter requestedLinkGetter = new RequestedLinkGetter(language, linkMatchers, getDumpByDate);
            try {
                HashMap<String, HashMap<String, List<URL>>> urls = requestedLinkGetter.getDumps();
                for (String dumpDate : urls.keySet()) {
                    for (String linkName : urls.get(dumpDate).keySet()) {
                        for (URL url : urls.get(dumpDate).get(linkName)) {
                            result.add(language.getLangCode() + "\t" + dumpDate + "\t" + linkName + "\t" + url);
                        }
                    }
                }
            } catch (WikapidiaException e) {
                System.err.println(e);
            }
        }

        if (!result.isEmpty()) {
            File file = new File(filePath);
            FileUtils.writeLines(file, result, "\n");
        }
    }
}
