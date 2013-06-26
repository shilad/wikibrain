package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
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

        PhraseAnalyzer analyzer = conf.get(PhraseAnalyzer.class, name);

        LOG.log(Level.INFO, "LOADING CORPUS FOR " + name);
        analyzer.loadCorpus();
        LOG.log(Level.INFO, "DONE");
    }
}
