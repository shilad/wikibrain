package org.wikibrain.core.cmd;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.utils.WpThreadUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * Captures common environment components for WikiBrain programs
 * and handles command-line argument parsing for them.
 *
 * @author Shilad Sen
 */
public class Env implements Closeable {
    // Hack: Logger is lazily configured to allow default logging configuration
    private static Logger LOG = null;

    private Configuration configuration;
    private Configurator configurator;

    /**
     * Parses standard command line arguments and builds the environment using them.
     */
    public Env() throws ConfigurationException {
        this(new HashMap<String, Object>());
    }

    /**
     * Creates a new environment, but folds in some external configuration files.
     */
    public Env(File ... pathConfs) throws ConfigurationException {
        this(new HashMap<String, Object>(), pathConfs);
    }

    /**
     * Parses standard command line arguments and builds the environment using them.
     */
    public Env(Map<String, Object> confParams, File ... pathConfs) throws ConfigurationException {
        if (LOG == null
        &&  System.getProperty("log4j.configurationFile") == null
        &&  (!confParams.containsKey("reconfigureLogging") || (Boolean)confParams.get("reconfigureLogging"))) {
            configureDefaultLogging();
        }

        // Hack delay until after first chance to configure logging
        if (LOG == null) LOG = LoggerFactory.getLogger(Env.class);

        // Load basic configuration
        configuration = new Configuration(confParams, pathConfs);
        configurator = new Configurator(configuration);

        // Set the max threads
        if (configuration.get().hasPath("maxThreads")) {
            int maxThreads = configuration.get().getInt("maxThreads");
            if (maxThreads > 0) {
                WpThreadUtils.setMaxThreads(maxThreads);
            }
        }

        // Set the temporary directory if it is specified
        if (configuration.get().hasPath("tmpDir")) {
            System.setProperty("java.io.tmpdir", configuration.get().getString("tmpDir"));
        }
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        if (pathConfs.length > 0) {
            LOG.info("using override configuration files " + Arrays.toString(pathConfs));
        }
        File baseDir = new File(configuration.get().getString("baseDir"));
        LOG.info("using baseDir " + baseDir.getAbsolutePath());
        LOG.info("using max vm heapsize of " + (Runtime.getRuntime().maxMemory() / (1024*1024)) + "MB");
        LOG.info("using languages " + getLanguages());
        LOG.info("using maxThreads " + WpThreadUtils.getMaxThreads());
        LOG.info("using tmpDir " + tmpDir);
    }

    private void configureDefaultLogging() {
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("log4j.configurationFile", "wikibrain-log4j2.yaml");
        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).reconfigure();
//        ((LoggerContext) LogManager.getContext(false)).updateLoggers();
        LOG = LoggerFactory.getLogger(Env.class);
        LOG.info("Configured default logging at the Info Level");
        LOG.info("To customize log4j2 set the 'log4j.configurationFile' system property or set EnvBuilder.setReconfigureLogging to false.");
    }

    public <T> T getComponent(Class<T> klass, String name) throws ConfigurationException {
        return getConfigurator().get(klass, name);
    }

    public <T> T getComponent(Class<T> klass) throws ConfigurationException {
        return getConfigurator().get(klass, "default");
    }

    public <T> T getComponent(Class<T> klass, String name, Language lang) throws ConfigurationException {
        return getConfigurator().get(klass, name, "language", lang.getLangCode());
    }

    public <T> T getComponent(Class<T> klass, Language lang) throws ConfigurationException {
        return getConfigurator().get(klass, "default", "language", lang.getLangCode());
    }

    public static <T> T getComponent(Configurator conf, Class<T> klass, Language lang) throws ConfigurationException {
        return conf.get(klass, "default", "language", lang.getLangCode());
    }

    public static <T> T getComponent(Configurator conf, Class<T> klass, String name, Language lang) throws ConfigurationException {
        return conf.get(klass, name, "language", lang.getLangCode());
    }

    public File getBaseDir() {
        return new File(configuration.getString("baseDir"));
    }

    public List<File> getFiles(FileMatcher ... matchers) {
        return getFiles(getLanguages(), matchers);
    }

    public List<File> getFiles(LanguageSet langs, FileMatcher ... matchers) {
        List<File> matches = new ArrayList<File>();
        for (Language l : langs) {
            for (FileMatcher fm : matchers) {
                List<File> f = getFiles(l, fm);
                if (f.isEmpty()) {
                    LOG.warn("no files matching language " + l + ", matcher " + fm.getName());
                }
                matches.addAll(f);
            }
        }
        return matches;
    }
    public List<File> getFiles(Language language, FileMatcher ... matchers) {
        return getFiles(new LanguageSet(language), matchers);
    }

    public List<File> getFiles(Language lang, FileMatcher fm) {
        return getFiles(lang, fm, configuration);
    }

    public static List<File> getFiles(Language lang, FileMatcher fm, Configuration configuration) {
        File downloadPath = new File(configuration.get().getString("download.path"));
        if (downloadPath == null) {
            throw new IllegalArgumentException("missing configuration for download.path");
        }
        if (LOG != null) LOG.debug("scanning download path " + downloadPath + " for files");
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

    public LanguageSet getLanguages() {
        try {
            return configurator.get(LanguageSet.class);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Configurator getConfigurator() {
        return configurator;
    }

    public int getMaxThreads() {
        return WpThreadUtils.getMaxThreads();
    }

    public Language getDefaultLanguage() {
        return getLanguages().getDefaultLanguage();
    }

    @Override
    public void close() throws IOException {
        configurator.close();
    }
}
