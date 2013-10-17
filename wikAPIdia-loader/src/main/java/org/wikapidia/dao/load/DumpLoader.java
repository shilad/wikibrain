package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.cmd.FileMatcher;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.xml.DumpPageXmlParser;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load the contents of a dump into the various daos.
 */
public class DumpLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());

    private final AtomicInteger counter = new AtomicInteger();
    private final LocalPageDao localPageDao;
    private final RawPageDao rawPageDao;
    private final MetaInfoDao metaDao;

    public DumpLoader(LocalPageDao localPageDao, RawPageDao rawPageDao, MetaInfoDao metaDao) {
        this.localPageDao = localPageDao;
        this.rawPageDao = rawPageDao;
        this.metaDao = metaDao;
    }

    /**
     * Expects file name format starting with lang + "wiki" for example, "enwiki"
     * @param file
     */
    public void load(File file) {
        Language lang = FileMatcher.ARTICLES.getLanguage(file.getAbsolutePath());
        DumpPageXmlParser parser = new DumpPageXmlParser(file,
                LanguageInfo.getByLanguage(lang));
        for (RawPage rp : parser) {
            if (counter.incrementAndGet() % 10000 == 0) {
                LOG.info("processing article " + counter.get());
            }
            save(file, rp);
        }
    }

    private void save(File file, RawPage rp) {
        try {
            rawPageDao.save(rp);
            metaDao.incrementRecords(rp.getClass(), rp.getLanguage());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);
            metaDao.incrementErrorsQuietly(rp.getClass(), rp.getLanguage());
        }
        try {
            LocalPage lp = new LocalPage(
                                rp.getLanguage(), rp.getLocalId(),
                                rp.getTitle(), rp.getNamespace(),
                                rp.isRedirect(), rp.isDisambig()
                            );
            localPageDao.save(lp);
            metaDao.incrementRecords(lp.getClass(), lp.getLanguage());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);
            metaDao.incrementErrorsQuietly(LocalPage.class, rp.getLanguage());
        }

    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("d"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();
        List<File> paths = env.getInputFiles(true, cmd.getArgList(), FileMatcher.ARTICLES);

        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        RawPageDao rpDao = conf.get(RawPageDao.class);
        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);

        final DumpLoader loader = new DumpLoader(lpDao, rpDao, metaDao);

        if (cmd.hasOption("d")) {
            lpDao.clear();
            rpDao.clear();
            metaDao.clear();
        }
        lpDao.beginLoad();
        rpDao.beginLoad();
        metaDao.clear();

        // loads multiple dumps in parallel
        ParallelForEach.loop(paths,
                new Procedure<File>() {
                    @Override
                    public void call(File path) throws Exception {
                        LOG.info("processing file: " + path);
                        loader.load(path);
                    }
                });

        lpDao.endLoad();
        rpDao.endLoad();
        metaDao.endLoad();
    }
}
