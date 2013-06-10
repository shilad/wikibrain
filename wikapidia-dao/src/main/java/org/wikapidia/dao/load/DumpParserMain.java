package org.wikapidia.dao.load;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.parser.wiki.WikiTextDumpParser;
import org.wikapidia.parser.xml.PageXml;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Load the contents of a
 */
public class DumpParserMain {
    private static final Logger LOG = Logger.getLogger(DumpParserMain.class.getName());
    private final List<ParserVisitor> visitors;
    private final AtomicInteger counter = new AtomicInteger();

    public DumpParserMain(List<ParserVisitor> visitors) {
        this.visitors = new ArrayList<ParserVisitor>(visitors);
        this.visitors.add(0, new ParserVisitor() {
            @Override
            public void beginPage(PageXml page) {
                if (counter.incrementAndGet() % 100 == 0) {
                    LOG.info("processing article " + counter.get());
                }
            }
        });
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
        LanguageInfo lang = LanguageInfo.getByLangCode(langCode);
        WikiTextDumpParser parser = new WikiTextDumpParser(file, lang);
        parser.parse(visitors);
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException {
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
            new HelpFormatter().printHelp("DumpParserMain", options);
            return;
        }


        File pathConf = cmd.hasOption("c") ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();

        // TODO: add other visitors
        LocalPageDao dao = (LocalPageDao) conf.get(LocalPageDao.class);
        visitors.add(new LocalPageLoader(dao));

        final DumpParserMain loader = new DumpParserMain(visitors);

        // TODO: initialize other visitors
        if (cmd.hasOption("t")) {
            dao.beginLoad();
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

        // TODO: finalize other visitors
        if (cmd.hasOption("i")) {
            dao.endLoad();
        }
    }
}
