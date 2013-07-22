package org.wikapidia.core.cmd;

import com.typesafe.config.Config;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Captures common environment components for WikAPIdia programs
 * and handles command-line argument parsing for them.
 *
 * @author Shilad Sen
 */
public class Env {
    private static final Logger LOG = Logger.getLogger(Env.class.getName());
    private final CommandLine cmd;

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
                        .withLongOpt("algorithmId")
                        .withDescription("universal concept map algorithm id")
                        .create("n"),
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

        // if an algorithm id is passed in the configuration file
        if (cmd.hasOption("n")) {
            System.setProperty("mapper.default", cmd.getOptionValue("a"));
        }

        // Load basic configuration
        File pathConf = cmd.hasOption('c') ? new File(cmd.getOptionValue('c')) : null;
        this.cmd = cmd;
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

    public List<File> getInputFiles(FileMatcher ... matchers) {
        return getInputFiles(false, matchers);
    }

    /**
     * Returns the list of already downloaded files for the input languages that
     * match the provided file matchers.
     *
     * @param useExtraArgs
     * @param matchers
     * @return
     */
    public List<File> getInputFiles(boolean useExtraArgs, FileMatcher ... matchers) {
        if (useExtraArgs && !cmd.getArgList().isEmpty()) {
            List<File> results = new ArrayList<File>();
            for (Object s : cmd.getArgList()) {
                results.add(new File((String)s));
            }
            return results;
        }

        File downloadPath = new File(configuration.get().getString("download.path"));
        if (downloadPath == null) {
            throw new IllegalArgumentException("missing configuration for download.path");
        }

        List<File> matches = new ArrayList<File>();
        for (Language l : languages) {
            for (FileMatcher fm : matchers) {
                List<File> f = getFiles(l, fm);
                if (f.isEmpty()) {
                    LOG.warning("no files matching language " + f + ", matcher " + fm.getName());
                }
                matches.addAll(f);
            }
        }
        return matches;
    }

    private List<File> getFiles(Language lang, FileMatcher fm) {
        File downloadPath = new File(configuration.get().getString("download.path"));
        if (downloadPath == null) {
            throw new IllegalArgumentException("missing configuration for download.path");
        }
        List<File> matchingFiles = new ArrayList<File>();
        File langDir = new File(downloadPath, lang.getLangCode());
        if (!langDir.isDirectory()) {
            return matchingFiles;
        }
        String mostRecent = null;
        for (File dateDir : langDir.listFiles((FileFilter) DirectoryFileFilter.INSTANCE)) {
            if (!dateDir.isDirectory()) {
                continue;
            }
            // skip if older than most recent
            if (mostRecent != null && dateDir.getName().compareTo(mostRecent) < 0) {
                continue;
            }
            List<File> lf = fm.matchFiles(Arrays.asList(dateDir.listFiles()));
            if (!lf.isEmpty()) {
                mostRecent = dateDir.getName();
                matchingFiles = lf;
            }
        }
        return matchingFiles;
    }

    public int getUniversalConceptAlgorithmId() {
        return getUniversalConceptAlgorithmId(configuration.get());
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

    public static int getUniversalConceptAlgorithmId(Config conf) {
        // look up mapper.default
        String path = conf.getString("mapper.default");
        // look up algorithmId under that.
        return conf.getInt("mapper."+path+".algorithmId");
    }
}
