package org.wikibrain.pageview;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LanguageSet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java "script" to load pageviews.
 * By default it loads two hours worth of pageviews from exactly one week ago.
 */
public class PageViewLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PageViewLoader.class);
    private final LanguageSet languageSet;
    private final PageViewDao dao;

    public PageViewLoader(LanguageSet languageSet, PageViewDao dao) {
        this.languageSet = languageSet;
        this.dao = dao;
    }

    public PageViewDao getDao() {
        return dao;
    }

    public void load(List<Interval> intervals) throws DaoException {
        dao.ensureLoaded(intervals, languageSet);
    }

    /**
     * start date and end date (second and third args) must be in UTC time
     * dates must be entered as four_digit_year-numeric_month_1-12-numeric_day_1-31-numeric_hour_0-23
     * can enter as many start date to end date combinations as desired in order to download multiple days
     * @param args
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @throws ConfigurationException
     * @throws WikiBrainException
     * @throws DaoException
     */
    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikiBrainException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("d"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("start-date")
                        .withDescription("date at which to start loading page view files")
                        .hasArgs()
                        .create("s"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("end-date")
                        .withDescription("date at which to stop loading page view files")
                        .hasArgs()
                        .create("e"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("random-hours")
                        .withDescription("select a certain number of random hours between the specified start and end date")
                        .hasArgs()
                        .create("r"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("PageViewLoader", options);
            System.exit(1);
            return;
        }

        List<Interval> intervals = new ArrayList<Interval>();
        if (cmd.hasOption("s")) {
            String [] startStrings = cmd.getOptionValues("s");
            String [] endStrings = cmd.hasOption("e") ? cmd.getOptionValues("e") : new String[0];
            for (int i = 0; i < startStrings.length; i++) {
                DateTime start = parseDateOrDie(startStrings[i]);
                DateTime end = (endStrings.length >= i)
                        ? parseDateOrDie(endStrings[i])
                        : start.plusMinutes(60 * 2 - 1);
                intervals.add(new Interval(start, end));
            }
            if (cmd.hasOption("r")) {
                if (startStrings.length != 1) {
                    System.err.println("when using -r, you can only specify one start/end interval");
                    new HelpFormatter().printHelp("PageViewLoader", options);
                    System.exit(1);
                }
                intervals = selectRandomHours(intervals.get(0), Integer.valueOf(cmd.getOptionValue("r")));
            }
        } else if (cmd.hasOption("r")) {
            // Default to 12 month date range.
            DateTime start = DateTime.now().minusMonths(13);
            DateTime end = DateTime.now().minusMonths(1);
            intervals = selectRandomHours(new Interval(start, end), Integer.valueOf(cmd.getOptionValue("r")));
        } else {
            // Default to two hours of views two week ago.
            DateTime start = DateTime.now().minusWeeks(2);
            DateTime end = start.plusMinutes(60 * 2 - 1);
            intervals.add(new Interval(start, end));
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();
        PageViewDao dao = conf.get(PageViewDao.class);
        PageViewLoader loader = new PageViewLoader(env.getLanguages(), dao);

        if (cmd.hasOption("d")) {
            LOG.info("Clearing pageview data");
            dao.clear();
        }

        LOG.info("loading pageview data for intervals " + intervals);

        loader.load(intervals);

        LOG.info("DONE");
    }

    public static String[] DATE_FORMATS = new String[] {"yyyy-MM-dd", "yyyy-MM-dd:HH"};


    private static DateTime parseDateOrDie(String dateString) throws WikiBrainException {
        DateTime result = null;
        for (String format : DATE_FORMATS) {
            DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
            try {
                result = fmt.parseDateTime(dateString);
            } catch (IllegalArgumentException e) {
                // Try the next format
            }
        }
        if (result == null) {
            System.err.format("Invalid date format '%s'. Must be one of %s.\n", dateString, StringUtils.join(DATE_FORMATS, ", "));
            System.exit(1);
        }
        return result;
    }

    private static List<Interval> selectRandomHours(Interval interval, int n) {
        Hours hours = interval.toDuration().toStandardHours();
        List<Interval> result = new ArrayList<Interval>();
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            int begOffset = random.nextInt(hours.getHours());
            DateTime start = interval.getStart().plusHours(begOffset);
            DateTime end = start.plusHours(1);
            result.add(new Interval(start, end));
        }
        return result;
    }
}

