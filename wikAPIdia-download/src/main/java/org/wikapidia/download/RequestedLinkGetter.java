package org.wikapidia.download;

import com.google.common.collect.Multimap;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.lang.Language;

import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.wikapidia.core.lang.LanguageSet;
import sun.security.provider.MD5;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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

    private static final Logger LOG = Logger.getLogger(RequestedLinkGetter.class.getName());
    private static final String DATE_FORMAT = "yyyyMMdd";

    private final Language lang;
    private final List<LinkMatcher> matchers;
    private final Date requestDate;    // This is the date requested by the user.

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
        URL langWikiPageUrl = new URL(DumpLinkGetter.BASEURL_STRING + "/" + lang.getLangCode().replace("-", "_") + "wiki/");
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

    /**
     * Get dump file links of the most recent available before the requestDate.
     * @return
     * @throws ParseException
     * @throws IOException
     * @throws WikapidiaException
     */
    protected Map<String, Multimap<LinkMatcher, DumpLinkInfo>> getDumps() throws ParseException, IOException, WikapidiaException {
        List<String> availableDates = availableDumpDatesSorted(getAllDates());
        Map<String, Multimap<LinkMatcher, DumpLinkInfo>> map = new HashMap<String, Multimap<LinkMatcher, DumpLinkInfo>>();
        List<LinkMatcher> unfoundMatchers = new ArrayList<LinkMatcher>(matchers);
        for (int i = availableDates.size() - 1; i > -1; i--) {
            DumpLinkGetter dumpLinkGetter = new DumpLinkGetter(lang, unfoundMatchers, availableDates.get(i));
            Multimap<LinkMatcher, DumpLinkInfo> batchDumps = dumpLinkGetter.getDumpFiles(dumpLinkGetter.getFileLinks());
            map.put(availableDates.get(i), batchDumps);
            for (int j = 0; j < unfoundMatchers.size(); j++) {
                LinkMatcher linkMatcher = unfoundMatchers.get(j);
                if (batchDumps.keySet().contains(linkMatcher)) {
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
    public static void main(String[] args) throws IOException, WikapidiaException, ParseException, ConfigurationException {

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("names")
                        .withDescription("Names of file types, separated by comma (e.g. 'articles,abstracts'). \nDefault is " + new Configuration().get().getStringList("download.matcher"))
                        .create("n"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("output")
                        .withDescription("Path to output file.")
                        .create("o"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("date")
                        .withDescription("Dumps are pulled from on or before this date. Default is today")
                        .create("d"));

        Env.addStandardOptions(options);
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("RequestedLinkGetter", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();

        List<LinkMatcher> linkMatchers = LinkMatcher.getListByNames(conf.getConf().get().getStringList("download.matcher"));
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

        LanguageSet languages = env.getLanguages();

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

        String filePath = conf.getConf().get().getString("download.listFile");
        if (cmd.hasOption('o')) {
            filePath = cmd.getOptionValue('o');
        }

        List<String> result = new ArrayList<String>();
        for (Language language : languages) {
            RequestedLinkGetter requestedLinkGetter = new RequestedLinkGetter(language, linkMatchers, getDumpByDate);
            try {
                Map<String, Multimap<LinkMatcher, DumpLinkInfo>> dumpLinks = requestedLinkGetter.getDumps();
                for (String dumpDate : dumpLinks.keySet()) {
                    for (LinkMatcher linkMatcher : dumpLinks.get(dumpDate).keySet()) {
                        for (DumpLinkInfo linkInfo : dumpLinks.get(dumpDate).get(linkMatcher)) {
                            result.add(linkInfo.getLanguage().getLangCode() + "\t" +
                                    linkInfo.getDate() + "\t" +
                                    linkInfo.getLinkMatcher().getName() + "\t" +
                                    linkInfo.getUrl() + "\t" +
                                    linkInfo.getMd5());
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
