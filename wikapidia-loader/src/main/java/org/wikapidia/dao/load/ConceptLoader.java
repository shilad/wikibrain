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
import java.util.logging.Logger;

/**
 */
public class ConceptLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());
    private final LanguageSet languageSet;
    private final Configurator configurator;

    public ConceptLoader(LanguageSet languageSet, Configurator configurator) {
        this.languageSet = languageSet;
        this.configurator = configurator;
    }

    public void load(ConceptMapper mapper) throws ConfigurationException, WikapidiaException {
        UniversalPageDao dao = configurator.get(UniversalPageDao.class);
        try {
            Iterable<UniversalPage> pages = mapper.getConceptMap(languageSet);
            dao.beginLoad();
            for (UniversalPage page : pages) {
                dao.save(page);
            }
            dao.endLoad();
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikapidiaException {
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
        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);
        loader.load(mapper);
    }
}
