package org.wikibrain.loader;

import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.phrases.PrunedCounts;
import org.wikibrain.phrases.StanfordPhraseAnalyzer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class PhraseLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PhraseLoader.class);

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikiBrainException, DaoException, InterruptedException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("analyzer")
                        .withDescription("the name of the phrase analyzer to use")
                        .create("p"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("ConceptLoader", options);
            return;
        }

        Map<String, String> confOverrides = new HashMap<String, String>();
        confOverrides.put("phrases.loading", "true");

        Env env = new EnvBuilder(cmd).setProperty("phrases.loading", true).build();
        List<String> toLoad = env.getConfiguration().get().getStringList("phrases.toLoad");
        if (cmd.hasOption("p")) {
            toLoad = Arrays.asList(cmd.getOptionValues("p"));
        }
        if (toLoad.contains("stanford")) {
            StanfordPhraseAnalyzer.downloadDictionaryIfNecessary(env.getConfiguration());
        }

        int n = 0;
        for (String name : toLoad) {
            PhraseAnalyzer analyzer = env.getConfigurator().get(PhraseAnalyzer.class, name);
            LOG.info("LOADING PHRASE CORPUS FOR " + name);
            n += analyzer.loadCorpus(env.getLanguages());
            LOG.info("DONE");
        }
        MetaInfoDao metaDao = env.getConfigurator().get(MetaInfoDao.class);
        for (Language lang : env.getLanguages()) {
            metaDao.incrementRecords(PrunedCounts.class, lang, n);
        }

        // For some reasons this appears to hang without this line.
        System.exit(0);
    }
}
