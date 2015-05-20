package org.wikibrain.loader;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.SizeFileComparator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.DumpSplitter;
import org.wikibrain.parser.WpParseException;
import org.wikibrain.parser.xml.PageXmlParser;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load the contents of a dump into the various daos.
 */
public class DumpLoader {
    private static final Logger LOG = LoggerFactory.getLogger(DumpLoader.class);
    public static final List<NameSpace> DEFAULT_NAMESPACES = Arrays.asList(NameSpace.ARTICLE, NameSpace.CATEGORY);

    private final AtomicInteger allPages = new AtomicInteger();
    private final AtomicInteger interestingPages = new AtomicInteger();

    private final Collection<NameSpace> nss;

    // If there are a maximum number of articles per language, langCounters will track counts per langauge
    private Integer maxPerLang = null;
    private final Map<Language, AtomicInteger> langCounters = new ConcurrentHashMap<Language, AtomicInteger>();

    private final LocalPageDao localPageDao;
    private final RawPageDao rawPageDao;
    private final MetaInfoDao metaDao;
    private TIntSet validIds = null;

    public DumpLoader(LocalPageDao localPageDao, RawPageDao rawPageDao, MetaInfoDao metaDao) {
        this(localPageDao, rawPageDao, metaDao, DEFAULT_NAMESPACES);
    }

    public DumpLoader(LocalPageDao localPageDao, RawPageDao rawPageDao, MetaInfoDao metaDao, Collection<NameSpace> nss) {
        this.localPageDao = localPageDao;
        this.rawPageDao = rawPageDao;
        this.metaDao = metaDao;
        this.nss = nss;
    }

    public void setValidIds(TIntSet validIds) {
        this.validIds = validIds;
    }

    /**
     * Expects file name format starting with lang + "wiki" for example, "enwiki"
     * @param file
     */
    public void load(final File file) {
        final Language lang = FileMatcher.ARTICLES.getLanguage(file.getAbsolutePath());
        if (!keepProcessingArticles(lang)) {
            return;
        }
        DumpSplitter parser = new DumpSplitter(file);
        ParallelForEach.iterate(
                parser.iterator(),
                WpThreadUtils.getMaxThreads(),
                1000,
                new Procedure<String>() {
                    @Override
                    public void call(String page) throws Exception {
                        try {
                            processOnePage(file, lang, page);
                        } catch (WpParseException e) {
                            LOG.warn("parsing of " + file.getPath() + " failed:", e);
                        }
                    }
                },
                Integer.MAX_VALUE
        );
    }

    private void processOnePage(File file, Language lang, String page) throws WpParseException {
        if (!keepProcessingArticles(lang)) {
            return;
        }
        if (allPages.incrementAndGet() % 10000 == 0) {
            LOG.info("processing article " + allPages.get() + " found " + interestingPages.get() + " interesting articles");
        }
        PageXmlParser parser = new PageXmlParser(LanguageInfo.getByLanguage(lang));
        RawPage rp = parser.parse(page);
        if (isInteresting(rp)) {
            interestingPages.incrementAndGet();
            save(file, rp);
            incrementLangCount(lang);
        }
    }

    private boolean isInteresting(RawPage rp) {
        if (rp == null || rp.getNamespace() == null) {
            return false;
        } else if (validIds != null && !validIds.contains(rp.getLocalId())) {
            return false;
        } else {
            return nss.contains(rp.getNamespace());
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
            LOG.warn("parsing of " + file + " failed:", e);
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
            LOG.warn("parsing of " + file + " failed:", e);
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
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("validIds")
                        .hasArg()
                        .withDescription("list of valid ids")
                        .create("v"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            System.exit(1);
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

        if (cmd.hasOption("v")) {
            TIntSet validIds = new TIntHashSet();
            for (String line : FileUtils.readLines(new File(cmd.getOptionValue("v")))) {
                validIds.add(Integer.valueOf(line.trim()));
            }
            loader.setValidIds(validIds);
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
        for (File path : paths) {
            LOG.info("processing file: " + path);
            loader.load(path);
        }

        lpDao.endLoad();
        rpDao.endLoad();
        metaDao.endLoad();
    }
}
