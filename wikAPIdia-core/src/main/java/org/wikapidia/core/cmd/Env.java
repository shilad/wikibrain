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
import org.wikapidia.utils.WpThreadUtils;

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

    private Configuration configuration;
    private Configurator configurator;

    /**
     * Parses standard command line arguments and builds the environment using them.
     * @throws ConfigurationException
     */
    public Env() throws ConfigurationException {
        this(new HashMap<String, Object>());
    }

    /**
     *
     * @param pathConfs
     * @throws ConfigurationException
     */
    public Env(File ... pathConfs) throws ConfigurationException {
        this(new HashMap<String, Object>(), pathConfs);
    }

    /**
     * Parses standard command line arguments and builds the environment using them.
     * @param confParams
     * @param pathConfs
     * @throws ConfigurationException
     */
    public Env(Map<String, Object> confParams, File ... pathConfs) throws ConfigurationException {
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

    public List<File> getFiles(FileMatcher ... matchers) {
        return getFiles(getLanguages(), matchers);
    }

    public List<File> getFiles(LanguageSet langs, FileMatcher ... matchers) {
        List<File> matches = new ArrayList<File>();
        for (Language l : langs) {
            for (FileMatcher fm : matchers) {
                List<File> f = getFiles(l, fm);
                if (f.isEmpty()) {
                    LOG.warning("no files matching language " + l + ", matcher " + fm.getName());
                }
                matches.addAll(f);
            }
        }
        return matches;
    }

    public List<File> getFiles(Language lang, FileMatcher fm) {
        return getFiles(lang, fm, configuration);
    }

    public static List<File> getFiles(Language lang, FileMatcher fm, Configuration configuration) {
        File downloadPath = new File(configuration.get().getString("download.path"));
        if (downloadPath == null) {
            throw new IllegalArgumentException("missing configuration for download.path");
        }
        LOG.fine("scanning download path " + downloadPath + " for files");
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
        return getUniversalConceptAlgorithmId(configuration);
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

    public static int getUniversalConceptAlgorithmId(Configuration conf) {
        // look up mapper.default
        String path = conf.get().getString("mapper.default");
        // look up algorithmId under that.
        return conf.get().getInt("mapper."+path+".algorithmId");
    }
}
