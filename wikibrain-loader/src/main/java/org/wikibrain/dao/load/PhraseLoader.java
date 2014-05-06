package org.wikibrain.dao.load;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.download.FileDownloader;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.phrases.StanfordPhraseAnalyzer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class PhraseLoader {
    private static final Logger LOG = Logger.getLogger(PhraseLoader.class.getName());

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

        for (String name : toLoad) {
            PhraseAnalyzer analyzer = env.getConfigurator().get(PhraseAnalyzer.class, name);
            LOG.log(Level.INFO, "LOADING PHRASE CORPUS FOR " + name);
            analyzer.loadCorpus(env.getLanguages());
            LOG.log(Level.INFO, "DONE");
        }
    }
}
