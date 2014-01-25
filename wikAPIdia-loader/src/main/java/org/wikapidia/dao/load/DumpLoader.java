package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.apache.commons.io.comparator.SizeFileComparator;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load the contents of a dump into the various daos.
 */
public class DumpLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());

    private final AtomicInteger counter = new AtomicInteger();

    // If there are a maximum number of articles per language, langCounters will track counts per langauge
    private Integer maxPerLang = null;
    private final Map<Language, AtomicInteger> langCounters = new ConcurrentHashMap<Language, AtomicInteger>();

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
        if (!keepProcessingArticles(lang)) {
            return;
        }
        DumpPageXmlParser parser = new DumpPageXmlParser(file,
                LanguageInfo.getByLanguage(lang));
        for (RawPage rp : parser) {
            if (counter.incrementAndGet() % 10000 == 0) {
                LOG.info("processing article " + counter.get());
            }
            save(file, rp);
            incrementLangCount(lang);
            if (!keepProcessingArticles(lang)) {
                break;
            }
        }
    }

    private boolean keepProcessingArticles(Language lang) {
        if (maxPerLang == null) {
            return true;
        } else if (!langCounters.containsKey(lang)) {
            return true;
        } else {
            return langCounters.get(lang).get() < maxPerLang;
        }
    }

    private void incrementLangCount(Language lang) {
        if (maxPerLang != null) {
            if (!langCounters.containsKey(lang)) {
                synchronized (langCounters) {
                    if (!langCounters.containsKey(lang)) {
                        langCounters.put(lang, new AtomicInteger());
                    }
                }
            }
            langCounters.get(lang).incrementAndGet();
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
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("max-articles")
                        .hasArg()
                        .withDescription("maximum articles per language")
                        .create("x"));
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

        EnvBuilder builder = new EnvBuilder(cmd);
        if (!builder.hasExplicitLanguageSet()) {
            builder.setUseDownloadedLanguages();
        }
        Env env = builder.build();
        Configurator conf = env.getConfigurator();
        List<File> paths;
        if (cmd.getArgList().isEmpty()) {
            paths = env.getFiles(FileMatcher.ARTICLES);
        } else {
            paths = new ArrayList<File>();
            for (Object arg : cmd.getArgList()) {
                paths.add(new File((String)arg));
            }
        }

        // Schedule the biggest files first to improve parallel performance
        Collections.sort(paths, SizeFileComparator.SIZE_REVERSE);

        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        RawPageDao rpDao = conf.get(RawPageDao.class);
        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);

        final DumpLoader loader = new DumpLoader(lpDao, rpDao, metaDao);
        if (cmd.hasOption("x")) {
            loader.maxPerLang = Integer.valueOf(cmd.getOptionValue("x"));
        }

        if (cmd.hasOption("d")) {
            lpDao.clear();
            rpDao.clear();
            metaDao.clear();
        }
        lpDao.beginLoad();
        rpDao.beginLoad();
        metaDao.beginLoad();

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
