package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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
                        .isRequired()
                        .withLongOpt("analyzer")
                        .withDescription("the name of the phrase analyzer to use")
                        .create("p"));
        Env.addStandardOptions(options);

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

        Env env = new Env(cmd, confOverrides);

        String name = cmd.getOptionValue("p");

        PhraseAnalyzer analyzer = env.getConfigurator().get(PhraseAnalyzer.class, name);

        LOG.log(Level.INFO, "LOADING PHRASE CORPUS FOR " + name);
        analyzer.loadCorpus(env.getLanguages());
        LOG.log(Level.INFO, "DONE");
    }
}
