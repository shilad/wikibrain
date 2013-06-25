package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;

import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 */
public class ConceptLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());
    private final LanguageSet languageSet;
    private final UniversalPageDao dao;

    public ConceptLoader(LanguageSet languageSet, UniversalPageDao dao) {
        this.languageSet = languageSet;
        this.dao = dao;
    }

    public void load(ConceptMapper mapper) throws ConfigurationException, WikapidiaException {
        try {
            Iterator<UniversalPage> pages = mapper.getConceptMap(languageSet);
            int i = 0;
            while (pages.hasNext()) {
                UniversalPage temp = pages.next();
                if (temp != null) {
                    dao.save(temp);
                    i++;
                }
                if (i%1000 == 0) System.out.println("UniversalPages mapped: " + i);
            }
            System.out.println("All UniversalPages mapped: " + i);
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikapidiaException, DaoException {
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
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("languages")
                        .withDescription("the set of languages to process")
                        .create("l"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("algorithm")
                        .withDescription("the name of the algorithm to execute")
                        .create("n"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("ConceptLoader", options);
            return;
        }
        File pathConf = cmd.hasOption("c") ? new File(cmd.getOptionValue('c')) : null;
        Configurator conf = new Configurator(new Configuration(pathConf));

        LanguageSet languages = LanguageSet.getSetOfAllLanguages();
        if (cmd.hasOption("l")) {
            String[] langCodes = cmd.getOptionValues("l");
            Collection<Language> langs = new ArrayList<Language>();
            for (String langCode : langCodes) {
                langs.add(Language.getByLangCode(langCode));
            }
            languages = new LanguageSet(langs);
        }

        String algorithm = null;
        if (cmd.hasOption("n")) {
            algorithm = cmd.getOptionValue("n");
        }

        UniversalPageDao dao = conf.get(UniversalPageDao.class);
        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);
        final ConceptLoader loader = new ConceptLoader(languages, dao);

        if (cmd.hasOption("t")) {
            dao.beginLoad();
            System.out.println("Begin Load");
        }

        loader.load(mapper);

        if (cmd.hasOption("i")) {
            dao.endLoad();
            System.out.println("End Load");
        }
    }
}
