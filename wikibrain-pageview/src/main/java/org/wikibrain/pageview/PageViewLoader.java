package org.wikibrain.pageview;

import org.apache.commons.cli.*;
import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.LanguageSet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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
    private LocalPageDao localPageDao;

    public PageViewLoader(LanguageSet languageSet, PageViewSqlDao dao, LocalPageDao localPageDao) {
        this.languageSet = languageSet;
        this.dao = dao;
        this.localPageDao=localPageDao;
    }

    public PageViewSqlDao getDao() {
        return dao;
    }

    public void load(DateTime startDate, DateTime endDate) throws ConfigurationException, WikiBrainException {
        double start = System.currentTimeMillis();
        try {
            LOG.log(Level.INFO, "Loading Page Views");
            PageViewIterator iterator = dao.getPageViewIterator(languageSet, startDate, endDate,localPageDao);
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
            LOG.info("Loading took " + elapsed + " minutes");
            LOG.log(Level.INFO, "All Page View files loaded: " + i);
        } catch (DaoException e) {
            double elapsed = (System.currentTimeMillis() - start) / 60000;
            LOG.info(elapsed + " minutes passed before exception thrown");
            throw new WikiBrainException(e);
        }
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


//        String startTime = args[1];
//        String endTime = args[2];

        ArrayList<DateTime[]> dates= new ArrayList<DateTime[]>();
        int numbOfDaysDownloading=(args.length-3);

        try {
            for (int i = 1; i < numbOfDaysDownloading; i=i+2) {
                dates.add(new DateTime[]{parseDate(args[i]),(parseDate(args[i+1]))});
            }
//            DateTime startDate = parseDate(startTime);
//            DateTime endDate = parseDate(endTime);

            CommandLineParser parser = new PosixParser();
            CommandLine cmd;

            Options opts = new Options();
            EnvBuilder.addStandardOptions(opts);
            try {
                cmd = parser.parse(opts, args);
            } catch (ParseException e) {
                System.err.println("Invalid option usage: " + e.getMessage());
//                new HelpFormatter().printHelp("WikidataDumpLoader", options);
                return;
            }

            Env env = EnvBuilder.envFromArgs(args);
            //Env env = new EnvBuilder(cmd).build();
            Configurator conf = env.getConfigurator();
            PageViewSqlDao dao = conf.get(PageViewSqlDao.class);
            LocalPageDao lpDao= conf.get(LocalPageDao.class);
            final PageViewLoader loader = new PageViewLoader(env.getLanguages(), dao, lpDao);

            /*if (cmd.hasOption("d")) {
                LOG.log(Level.INFO, "Clearing data");
                dao.clear();
            } */

            LOG.log(Level.INFO, "Clearing data");
            dao.clear();

            LOG.log(Level.INFO, "Begin Load");
            dao.beginLoad();

            for(DateTime[] startEndDate:dates){
                loader.load(startEndDate[0],startEndDate[1]);
//                loader.load(startDate, endDate);
            }


            LOG.log(Level.INFO, "End Load");
            dao.endLoad();
            LOG.log(Level.INFO, "DONE");
        } catch (WikiBrainException wE) {
            System.err.println("Invalid option usage:" + wE.getMessage());
            //new HelpFormatter().printHelp("PageViewLoader", options);
            return;
        }
    }

    private static DateTime parseDate(String dateString) throws WikiBrainException {
        if (dateString == null) {
            throw new WikiBrainException("Need to specify start and end date");
        }
        String[] dateElems = dateString.split("-");
        for (String de: dateElems){
//            System.out.println(de);
        }
        try {
            int year = Integer.parseInt(dateElems[0]);
            int month = Integer.parseInt(dateElems[1]);
            int day = Integer.parseInt(dateElems[2]);
            int hour = Integer.parseInt(dateElems[3]);
            return new DateTime(year, month, day, hour, 0);
        } catch (Exception e) {
            System.out.println(e.toString());
            throw new WikiBrainException("Start and end dates must be entered in the following format (hyphen-delimited):\n" +
                    "<four_digit_year>-<numeric_month_1-12>-<numeric_day_1-31>-<numeric_hour_0-23>");
        }
    }
}

