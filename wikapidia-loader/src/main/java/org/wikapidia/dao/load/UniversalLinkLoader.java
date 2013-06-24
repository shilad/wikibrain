package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.mapper.ConceptMapper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 */
public class UniversalLinkLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());

    private final LanguageSet languageSet;
    private final LocalLinkDao localLinkDao;
    private final UniversalPageDao universalPageDao;
    private final UniversalLinkDao universalLinkDao;

    public UniversalLinkLoader(LanguageSet languageSet, LocalLinkDao localLinkDao, UniversalPageDao universalPageDao, UniversalLinkDao universalLinkDao) {
        this.languageSet = languageSet;
        this.localLinkDao = localLinkDao;
        this.universalPageDao = universalPageDao;
        this.universalLinkDao = universalLinkDao;
    }

    /**
     * Loads the database of UniversalLinks. Requires a database of UniversalPages and LocalLinks
     * @throws DaoException
     */
    public void loadLinkMap(int algorithmId) throws WikapidiaException {
        try {
            Iterable<LocalLink> localLinks = localLinkDao.get(new DaoFilter().setLanguages(languageSet));
            int i=0;
            for (LocalLink localLink : localLinks) {
                i++;
                universalLinkDao.save(
                        localLink,
                        universalPageDao.getUnivPageId(
                                localLink.getLanguage(),
                                localLink.getSourceId(),
                                algorithmId),
                        universalPageDao.getUnivPageId(
                                localLink.getLanguage(),
                                localLink.getDestId(),
                                algorithmId),
                        algorithmId
                );
                if (i%1000 == 0) {
                    System.out.println("UniversalLinks loaded: " + i);
                }
            }
            System.out.println("All UniversalLinks loaded: " + i);
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
                        .withLongOpt("languages")
                        .withDescription("the set of languages to process")
                        .create("l"));
        options.addOption(
                new DefaultOptionBuilder()
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

        LocalLinkDao localLinkDao = conf.get(LocalLinkDao.class);
        UniversalPageDao universalPageDao = conf.get(UniversalPageDao.class);
        UniversalLinkDao universalLinkDao = conf.get(UniversalLinkDao.class);
        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);
        final UniversalLinkLoader loader = new UniversalLinkLoader(
                languages,
                localLinkDao,
                universalPageDao,
                universalLinkDao);

        if (cmd.hasOption("t")) {
            localLinkDao.beginLoad();
            universalPageDao.beginLoad();
            universalLinkDao.beginLoad();
            System.out.println("Begin Load");
        }

        loader.loadLinkMap(mapper.getId());

        if (cmd.hasOption("i")) {
            localLinkDao.endLoad();
            universalPageDao.endLoad();
            universalLinkDao.endLoad();
            System.out.println("End Load");
        }
    }
}
