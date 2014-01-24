package org.wikapidia.pageview;

import org.apache.commons.cli.*;
import org.joda.time.DateTime;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.sql.LocalCategoryMemberSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 1/2/14
 * Time: 11:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class PageViewLoader {
    private static final Logger LOG = Logger.getLogger(PageViewLoader.class.getName());
    private final LanguageSet languageSet;
    private final PageViewSqlDao dao;

    public PageViewLoader(LanguageSet languageSet, PageViewSqlDao dao) {
        this.languageSet = languageSet;
        this.dao = dao;
    }

    public PageViewSqlDao getDao() {
        return dao;
    }

    public void load(DateTime startDate, DateTime endDate) throws ConfigurationException, WikapidiaException {
        double start = System.currentTimeMillis();
        try {
            LOG.log(Level.INFO, "Loading Page Views");
            PageViewIterator iterator = dao.getPageViewIterator(languageSet, startDate, endDate);
            int i = 0;
            while (iterator.hasNext()) {
                List<PageViewDataStruct> dataStructs = iterator.next();
                for (PageViewDataStruct data : dataStructs) {
                    dao.addData(data);
                }
                i++;
                if (i % 24 == 0) {
                    double elapsed = (System.currentTimeMillis() - start) / 60000;
                    LOG.log(Level.INFO, "Loaded " + (i/24) + " days worth of Page View files in " + elapsed + " minutes");
                }
                if (i % 744 == 0) {
                    double elapsed = (System.currentTimeMillis() - start) / 60000;
                    LOG.log(Level.INFO, "Loaded " + (i/744) + "months worth of Page View files in " + elapsed + " minutes");
                }
            }
            double elapsed = (System.currentTimeMillis() - start) / 60000;
            System.out.println("Loading took " + elapsed + " minutes");
            LOG.log(Level.INFO, "All Page View files loaded: " + i);
        } catch (DaoException e) {
            double elapsed = (System.currentTimeMillis() - start) / 60000;
            System.out.println(elapsed + " minutes passed before exception thrown");
            throw new WikapidiaException(e);
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikapidiaException, DaoException {
        /*Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("d"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("start-date")
                        .withDescription("date at which to start loading page view files")
                        .isRequired()
                        .hasArg()
                        .create("s"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("end-date")
                        .withDescription("date at which to stop loading page view files")
                        .isRequired()
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
        }*/

        //String startTime = cmd.getOptionValue("s", null);
        String startTime = args[1];
        String endTime = args[2];

        try {
            DateTime startDate = parseDate(startTime);
            DateTime endDate = parseDate(endTime);

            Env env = new EnvBuilder().setLanguages(args[0]).build();
            //Env env = new EnvBuilder(cmd).build();
            Configurator conf = env.getConfigurator();
            PageViewSqlDao dao = conf.get(PageViewSqlDao.class);
            final PageViewLoader loader = new PageViewLoader(env.getLanguages(), dao);

            /*if (cmd.hasOption("d")) {
                LOG.log(Level.INFO, "Clearing data");
                dao.clear();
            } */

            LOG.log(Level.INFO, "Clearing data");
            dao.clear();

            LOG.log(Level.INFO, "Begin Load");
            dao.beginLoad();

            loader.load(startDate, endDate);

            LOG.log(Level.INFO, "End Load");
            dao.endLoad();
            LOG.log(Level.INFO, "DONE");
        } catch (WikapidiaException wE) {
            System.err.println("Invalid option usage:" + wE.getMessage());
            //new HelpFormatter().printHelp("PageViewLoader", options);
            return;
        }
    }

    private static DateTime parseDate(String dateString) throws WikapidiaException {
        if (dateString == null) {
            throw new WikapidiaException("Need to specify start and end date");
        }
        String[] dateElems = dateString.split("-");
        try {
            int year = Integer.parseInt(dateElems[0]);
            int month = Integer.parseInt(dateElems[1]);
            int day = Integer.parseInt(dateElems[2]);
            int hour = Integer.parseInt(dateElems[3]);
            return new DateTime(year, month, day, hour, 0);
        } catch (Exception e) {
            throw new WikapidiaException("Start and end dates must be entered in the following format (hypen-delimited):\n" +
                    "<four_digit_year>-<numeric_month_1-12>-<numeric_day_1-31>-<numeric_hour_0-23");
        }
    }
}

