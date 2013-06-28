package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.phrases.NormalizedStringPruner;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.phrases.SimplePruner;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class PhraseLoader {
    private static final Logger LOG = Logger.getLogger(PhraseLoader.class.getName());

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
                        .hasArg()
                        .isRequired()
                        .withLongOpt("analyzer")
                        .withDescription("the name of the phrase analyzer to use")
                        .create("n"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("languages")
                        .withDescription("the set of languages to process")
                        .create("l"));

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

        System.setProperty("phrases.loading", "true");
        Configuration c = new Configuration(pathConf);
        Configurator conf = new Configurator(c);

        String name = null;
        if (cmd.hasOption("n")) {
            name = cmd.getOptionValue("n");
        }
        LanguageSet langs;
        if (cmd.hasOption("l")) {
            langs = new LanguageSet(Arrays.asList(cmd.getOptionValues("l")));
        } else {
            langs = new LanguageSet((List<String>)conf.getConf().get().getAnyRef("Languages"));
        }

        PhraseAnalyzer analyzer = conf.get(PhraseAnalyzer.class, name);

        LOG.log(Level.INFO, "LOADING PHRASE CORPUS FOR " + name);
        int minCount = c.get().getInt("phrases.pruning.minCount");
        int maxRank = c.get().getInt("phrases.pruning.maxRank");
        double minFraction = c.get().getDouble("phrases.pruning.minFraction");

        analyzer.loadCorpus(
                langs,
                new NormalizedStringPruner(minCount, maxRank, minFraction),
                new SimplePruner<Integer>(minCount, maxRank, minFraction)
        );
        LOG.log(Level.INFO, "DONE");
    }
}
