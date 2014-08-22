package org.wikibrain.pageview;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
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
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java "script" to load pageviews.
 * By default it loads two hours worth of pageviews from exactly one week ago.
 */
public class PageViewLoader {
    private static final Logger LOG = Logger.getLogger(PageViewLoader.class.getName());
    private final LanguageSet languageSet;
    private final PageViewDao dao;

    public PageViewLoader(LanguageSet languageSet, PageViewDao dao) {
        this.languageSet = languageSet;
        this.dao = dao;
    }

    public PageViewDao getDao() {
        return dao;
    }

    public void load(DateTime startDate, DateTime endDate) throws DaoException {
        dao.ensureLoaded(startDate, endDate, languageSet);
    }

    /**
     * first arg: comma-separated lang codes
     * start date and end date (second and third args) must be in UTC time
     * dates must be entered as <four_digit_year>-<numeric_month_1-12>-<numeric_day_1-31>-<numeric_hour_0-23>
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
                        .hasArg()
                        .create("s"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("end-date")
                        .withDescription("date at which to stop loading page view files")
                        .hasArg()
                        .create("e"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("PageViewLoader", options);
            return;
        }

        /**
         * By default, load one days worth of data from a week ago.
         */
        DateTime start = DateTime.now().minusWeeks(1);
        if (cmd.hasOption("s")) {
            start = parseDateOrDie(cmd.getOptionValue("s"));
        }

        DateTime end = start.plusMinutes(60 * 2 - 1);
        if (cmd.hasOption("e")) {
            end = parseDateOrDie(cmd.getOptionValue("e"));
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();
        PageViewSqlDao dao = conf.get(PageViewSqlDao.class);
        PageViewLoader loader = new PageViewLoader(env.getLanguages(), dao);

        if (cmd.hasOption("d")) {
            LOG.info("Clearing pageview data");
            dao.clear();
        }

        SortedSet<DateTime> tstamps = PageViewUtils.timestampsInInterval(start, end);
        LOG.info("loading pageview data for " + tstamps.size() + " hours");

        loader.load(start, end);

        LOG.log(Level.INFO, "DONE");
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
}

