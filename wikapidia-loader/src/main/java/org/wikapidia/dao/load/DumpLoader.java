package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.parser.wiki.WikiTextDumpParser;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.xml.DumpPageXmlParser;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load the contents of a dump into the various daos.
 */
public class DumpLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());
    private static final String[] DUMP_SUFFIXES = { "xml", "xml.bz2", "xml.gz", "xml.7z" };

    private final AtomicInteger counter = new AtomicInteger();
    private final LocalPageDao localPageDao;
    private final RawPageDao rawPageDao;

    public DumpLoader(LocalPageDao localPageDao, RawPageDao rawPageDao) {
        this.localPageDao = localPageDao;
        this.rawPageDao = rawPageDao;
    }

    /**
     * Expects file name format starting with lang + "wiki" for example, "enwiki"
     * @param file
     */
    public void load(File file) {
        int i = file.getName().indexOf("wiki");
        if (i < 0) {
            throw new IllegalArgumentException("invalid filename. Expected prefix, for example 'enwiki-...'");
        }
        String langCode = file.getName().substring(0, i);
        langCode = langCode.replace('_', '-');
        LanguageInfo lang = LanguageInfo.getByLangCode(langCode);
        DumpPageXmlParser parser = new DumpPageXmlParser(file, lang);
        for (RawPage rp : parser) {
            if (counter.incrementAndGet() % 1000 == 0) {
                LOG.info("processing article " + counter.get());
            }
            save(file, rp);
        }
    }

    private void save(File file, RawPage rp) {
        try {
            rawPageDao.save(rp);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);

        }
        try {
            LocalPage lp = new LocalPage(
                                rp.getLang(), rp.getPageId(),
                                rp.getTitle(), rp.getNamespace(),
                                rp.isRedirect(), rp.isDisambig()
                            );
            localPageDao.save(lp);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);
        }

    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("t"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("create-indexes")
                        .withDescription("create all indexes after loading")
                        .create("i"));
        Env.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();

        List<String> langCodes;
        if (cmd.hasOption("l")) {
            langCodes = Arrays.asList(cmd.getOptionValues("l"));
        } else {
            langCodes = conf.getConf().get().getStringList("languages");
        }

        File downloadPath = new File(conf.getConf().get().getString("download.path"));
        List<String> dumps = new ArrayList<String>();
        if (!cmd.getArgList().isEmpty()) {                                          // There are files specified
            dumps = cmd.getArgList();
        } else {                                                                    // No specified files
            if ((!downloadPath.isDirectory() || downloadPath.list().length == 0)) { // Default path is missing or empty
                System.err.println( "There is no download path. Please specify one or configure a default.");
                new HelpFormatter().printHelp("DumpLoader", options);
                return;
            } else {                                                                        // Default path is functional
                for (File langDir : downloadPath.listFiles()) {                             // Layered for-loops sift through
                    if (langDir.isDirectory() && langCodes.contains(langDir.getName())) {   // the directory structure of the
                        for (File f : FileUtils.listFiles(langDir, DUMP_SUFFIXES, true)) {  // download process:
                            dumps.add(f.getPath());                                         // ${PARENT}/langcode/date/dumpfile.xml.bz2
                        }
                    }
                }
            }
        }

        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        RawPageDao rpDao = conf.get(RawPageDao.class);

        final DumpLoader loader = new DumpLoader(lpDao, rpDao);

        if (cmd.hasOption("t")) {
            lpDao.beginLoad();
            rpDao.beginLoad();
        }

        // loads multiple dumps in parallel
        ParallelForEach.loop(dumps,
                env.getMaxThreads(),
                new Procedure<String>() {
                    @Override
                    public void call(String path) throws Exception {
                        loader.load(new File(path));
                    }
                });

        if (cmd.hasOption("i")) {
            lpDao.endLoad();
            rpDao.endLoad();
        }
    }
}
