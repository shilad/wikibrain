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
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 */
public class ConceptLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());
    private final LanguageSet languageSet;
//    private final List<ParserVisitor> visitors;
//    private final AtomicInteger counter = new AtomicInteger();

    public ConceptLoader(LanguageSet languageSet, Configurator configurator) {
        this.languageSet = languageSet;
    }

    public void load(String algorithm) {

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
        File pathConf = cmd.hasOption("c") ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

//        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();
//
//        // TODO: add other visitors
//        LocalPageDao lpDao = conf.get(LocalPageDao.class);
//        RawPageDao rpDao = conf.get(RawPageDao.class);
//        LocalLinkDao llDao = conf.get(LocalLinkDao.class);
//        visitors.add(new LocalPageLoader(lpDao));
//        visitors.add(new RawPageLoader(rpDao));
//        visitors.add(new LocalLinkLoader(llDao));
//
//        // TODO: initialize other visitors
//        if (cmd.hasOption("t")) {
//            lpDao.beginLoad();
//            rpDao.beginLoad();
//            llDao.beginLoad();
//        }
//
//        // TODO: finalize other visitors
//        if (cmd.hasOption("i")) {
//            lpDao.endLoad();
//            rpDao.endLoad();
//            llDao.endLoad();
//        }

        LanguageSet languages = LanguageSet.getSetOfAllLanguages();
        if (cmd.hasOption("l")) {
            String[] langCodes = cmd.getOptionValues("l");
            Collection<Language> langs = new ArrayList<Language>();
            for (String langCode : langCodes) {
                langs.add(Language.getByLangCode(langCode));
            }
            languages = new LanguageSet(langs);
        }

        String algorithm = "monolingual";
        if (cmd.hasOption("n")) {
            algorithm = cmd.getOptionValue("n");
        }

        final ConceptLoader loader = new ConceptLoader(languages);
    }
}
