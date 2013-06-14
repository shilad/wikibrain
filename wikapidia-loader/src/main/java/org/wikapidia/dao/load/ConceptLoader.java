package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.dao.DaoException;

import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.algorithms.MonolingualConceptMapper;
import org.wikapidia.mapper.utils.MapperIterable;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 */
public class ConceptLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());
    private final LanguageSet languageSet;
    private final Configurator configurator;
//    private final AtomicInteger counter = new AtomicInteger();

    public ConceptLoader(LanguageSet languageSet, Configurator configurator) {
        this.languageSet = languageSet;
        this.configurator = configurator;
    }

    public void load(String algorithm) throws ConfigurationException, DaoException {
        if (algorithm.equalsIgnoreCase("monolingual")) {
            UniversalPageDao dao = configurator.get(UniversalPageDao.class);
            MapperIterable<UniversalPage> pages = new MonolingualConceptMapper(configurator).getConceptMap(languageSet);
            dao.beginLoad();
            for (UniversalPage page : pages) {
                dao.save(page);
            }
            dao.endLoad();
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
        File pathConf = cmd.hasOption("c") ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

//        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();

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

        final ConceptLoader loader = new ConceptLoader(languages, conf);

        UniversalPageDao dao = conf.get(UniversalPageDao.class);

//        if (cmd.hasOption("t")) {
//            dao.beginLoad();
//        }

        loader.load(algorithm);

//        if (cmd.hasOption("i")) {
//            dao.endLoad();
//        }
    }
}
