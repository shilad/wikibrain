package org.wikibrain.download;

import com.google.common.collect.Multimap;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.lang.Language;

import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.wikibrain.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Get URLs of the dump file links with specified language, type of dump file and the date before which the dumps
 * are pulled.
 *
 * @author Ari Weiland
 * @author Yulun Li
 *
 */
public class RequestedLinkGetter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestedLinkGetter.class);
    private static final String DATE_FORMAT = "yyyyMMdd";

    private final Language lang;
    private final List<FileMatcher> matchers;
    private final Date requestDate;    // This is the date requested by the user.

    public RequestedLinkGetter(Language lang, List<FileMatcher> matchers, Date requestDate) {
        this.lang = lang;
        this.matchers = matchers;
        this.requestDate = requestDate;
    }

    /**
     * Return a sorted list of dump dates before the date requested.
     * @return list of dates as String.
     */
    protected List<String> getAllDates() throws IOException, ParseException, WikiBrainException {
        URL langWikiPageUrl = new URL(DumpLinkGetter.BASEURL_STRING + "/" + lang.getLangCode().replace("-", "_") + "wiki/");
        Document doc = Jsoup.parse(IOUtils.toString(langWikiPageUrl.openStream()));
        Elements availableDates = doc.select("body").select("pre").select("a[href]");
        List<Date> dates = new ArrayList<Date>();
        for (Element element : availableDates) {
            Matcher dateMatcher = Pattern.compile("(\\d{8})/").matcher(element.attr("href"));
            while (dateMatcher.find()) {
                dates.add(stringToDate(dateMatcher.group(1)));
            }
        }

        Collections.sort(dates, new Comparator<Date>() {
            public int compare(Date date1, Date date2) {
                return date1.compareTo(date2);
            }
        });
        List<String> dateListSorted = new ArrayList<String>();
        for (Date date : dates) {
            if (!date.after(requestDate)) {
                dateListSorted.add(new SimpleDateFormat(DATE_FORMAT).format(date));
            }
        }
        if (dateListSorted.isEmpty()) {
            throw new WikiBrainException("No dumps for " + lang.getLangCode() + " found before " + new SimpleDateFormat(DATE_FORMAT).format(requestDate));
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
     * @throws WikiBrainException
     */
    protected Map<String, Multimap<FileMatcher, DumpLinkInfo>> getDumps() throws ParseException, IOException, WikiBrainException {
        List<String> availableDates = getAllDates();
        Map<String, Multimap<FileMatcher, DumpLinkInfo>> map = new HashMap<String, Multimap<FileMatcher, DumpLinkInfo>>();
        List<FileMatcher> unfoundMatchers = new ArrayList<FileMatcher>(matchers);
        for (int i = availableDates.size() - 1; i > -1; i--) {
            DumpLinkGetter dumpLinkGetter = new DumpLinkGetter(lang, unfoundMatchers, availableDates.get(i));
            Multimap<FileMatcher, DumpLinkInfo> batchDumps = dumpLinkGetter.getDumpFiles(dumpLinkGetter.getFileLinks());
            map.put(availableDates.get(i), batchDumps);
            for (int j = 0; j < unfoundMatchers.size(); j++) {
                FileMatcher linkMatcher = unfoundMatchers.get(j);
                if (batchDumps.keySet().contains(linkMatcher)) {
                    unfoundMatchers.remove(linkMatcher);
                    j--;
                }
            }
            if (unfoundMatchers.isEmpty()) {
                return map;
            }
            if (i == 0) {
                LOG.warn("Some matchers not found: " + unfoundMatchers);
            }
        }
        return map;
    }

    public List<String> getLangLinks() throws WikiBrainException, IOException, ParseException {
        List<String> result = new ArrayList<String>();
        Map<String, Multimap<FileMatcher, DumpLinkInfo>> dumpLinks = this.getDumps();
        for (String dumpDate : dumpLinks.keySet()) {
            for (FileMatcher linkMatcher : dumpLinks.get(dumpDate).keySet()) {
                for (DumpLinkInfo linkInfo : dumpLinks.get(dumpDate).get(linkMatcher)) {
                    result.add(linkInfo.getLanguage().getLangCode() + "\t" +
                            linkInfo.getDate() + "\t" +
                            linkInfo.getLinkMatcher().getName() + "\t" +
                            linkInfo.getCounter() + "\t" +
                            linkInfo.getUrl() + "\t" +
                            linkInfo.getMd5());
                }
            }
        }
        return result;
    }

    /**
     * Parse command line and generate .tsv file containing language code, date of dump, name of file type and link url.
     * @param args command line prompt
     * @throws IOException
     * @throws WikiBrainException
     * @throws ParseException
     */
    public static void main(String[] args) throws IOException, WikiBrainException, ParseException, ConfigurationException {

        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("names")
                        .withDescription("Names of file types, separated by comma (e.g. 'articles,abstracts'). \nDefault is " + new Configuration().get().getStringList("download.matcher"))
                        .create("f"));
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
                        .create("y"));

        EnvBuilder.addStandardOptions(options);

        // You MUST specify a language set when downloading files
        Option o = options.getOption("l");
        o.setRequired(true);
        options.addOption(o);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("RequestedLinkGetter", options);
            System.exit(1);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();

        List<FileMatcher> linkMatchers;
        if (cmd.hasOption("n")) {
            linkMatchers = new ArrayList<FileMatcher>();
            for (String name : cmd.getOptionValues("n")) {
                FileMatcher matcher = FileMatcher.getByName(name);
                if (matcher == null) {
                    System.err.println("Invalid matcher name: " + name + "\nValid matcher names: \n" + FileMatcher.getAllNames().toString());
                    System.exit(1);
                }
                linkMatchers.add(matcher);
            }
        } else {
            linkMatchers = FileMatcher.getListByNames(conf.getConf().get().getStringList("download.matcher"));
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
        LOG.info("writing download list to " + filePath);

        List<String> result = new ArrayList<String>();
        for (Language language : languages) {
            RequestedLinkGetter getter = new RequestedLinkGetter(
                    language, linkMatchers, getDumpByDate);
            result.addAll(getter.getLangLinks());
        }
        if (languages.size() >= 2) {
            RequestedLinkGetter getter = new RequestedLinkGetter(
                    Language.WIKIDATA,
                    Arrays.asList(FileMatcher.WIKIDATA_ITEMS),
                    getDumpByDate);
            result.addAll(getter.getLangLinks());
        }

        if (!result.isEmpty()) {
            File file = new File(filePath);
            FileUtils.writeLines(file, result, "\n");
        }
    }
}
