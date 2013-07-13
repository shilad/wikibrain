package org.wikapidia.core.cmd;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.lang.LanguageSet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class Env {
    private static final Logger LOG = Logger.getLogger(Env.class.getName());

    private LanguageSet languages;
    private Configuration configuration;
    private Configurator configurator;
    private int maxThreads = Runtime.getRuntime().availableProcessors();

    /**
     * Adds the standard command line options to an options argument.
     * @param options
     */
    public static void addStandardOptions(Options options) {
        Option toAdd[] = new Option[] {
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("conf")
                        .withDescription("configuration file")
                        .create("c"),
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("threads")
                        .withDescription("the maximum number of threads that should be used")
                        .create("h"),
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("languages")
                        .withDescription("the set of languages to process, separated by commas")
                        .create("l")
        };
        for (Option o : toAdd) {
            if (options.hasOption(o.getOpt())) {
                throw new IllegalArgumentException("Standard command line option " + o.getOpt() + " reused");
            }
            options.addOption(o);
        }
    }

    /**
     * Parses standard command line arguments and builds the environment using them.
     * @param cmd
     * @throws ConfigurationException
     */
    public Env(CommandLine cmd) throws ConfigurationException {
        this(cmd, new HashMap<String, String>());
    }

    /**
     * Parses standard command line arguments and builds the environment using them.
     * @param cmd
     * @throws ConfigurationException
     */
    public Env(CommandLine cmd, Map<String, String> confOverrides) throws ConfigurationException {
        // Override configuration parameters using system properties
        for (String key : confOverrides.keySet()) {
            System.setProperty(key, confOverrides.get(key));
        }

        // Load basic configuration
        File pathConf = cmd.hasOption('c') ? new File(cmd.getOptionValue('c')) : null;
        configuration = new Configuration(pathConf);
        configurator = new Configurator(configuration);

        // Load languages
        if (cmd.hasOption("l")) {
            languages = new LanguageSet(cmd.getOptionValue("l"));
        } else {
            languages = new LanguageSet(configuration.get().getStringList("languages"));
        }

        // Load numThreads
        if (cmd.hasOption("h")) {
            maxThreads = new Integer(cmd.getOptionValue("h"));
        }

        LOG.info("using languages " + languages);
        LOG.info("using maxThreads " + maxThreads);
    }

    public LanguageSet getLanguages() {
        return languages;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Configurator getConfigurator() {
        return configurator;
    }

    public int getMaxThreads() {
        return maxThreads;
    }
}
