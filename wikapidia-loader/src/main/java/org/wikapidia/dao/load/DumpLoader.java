package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
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
                        .hasArg()
                        .withLongOpt("conf")
                        .withDescription("configuration file")
                        .create("c"));
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

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            return;
        }
        File pathConf = cmd.hasOption('c') ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();

        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        RawPageDao rpDao = conf.get(RawPageDao.class);

        final DumpLoader loader = new DumpLoader(lpDao, rpDao);

        if (cmd.hasOption("t")) {
            lpDao.beginLoad();
            rpDao.beginLoad();
        }

        // loads multiple dumps in parallel
        ParallelForEach.loop(cmd.getArgList(),
                Runtime.getRuntime().availableProcessors(),
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
