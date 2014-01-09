package org.wikapidia.sr.dataset;

import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.utils.WpIOUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Reads and writes datasets.
 *
 * Supports reading builtin datasets from resource files.
 *
 * @author Shilad Sen
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class DatasetDao {
    private static final Logger LOG = Logger.getLogger(Dataset.class.getName());

    public static final String RESOURCE_DATSET = "/datasets";
    public static final String RESOURCE_DATASET_INFO = "/datasets/info.tsv";

    private final Collection<Info> info;
    private Map<String, List<String>> groups = new HashMap<String, List<String>>();
    private boolean normalize = true; // If true, normalize all scores to [0,1]
    private boolean resolvePhrases = false;
    private Disambiguator disambiguator = null;

    /**
     * Information about a particular dataset
     */
    public static class Info {
        private String name;
        private LanguageSet languages;

        public Info(String name, LanguageSet languages) {
            this.name = name;
            this.languages = languages;
        }

        public String getName() { return name; }
        public LanguageSet getLanguages() { return languages; }
    }

    /**
     * Creates a new dataset dao with particular configuration information.
     */
    public DatasetDao() {
        try {
            this.info = readInfos();
        } catch (DaoException e) {
            throw new RuntimeException(e);  // errors shouldn't occur for compiled resources
        }
    }

    /**
     * Creates a new dataset dao with particular configuration information.
     * @param info
     */
    public DatasetDao(Collection<Info> info) {
        this.info = info;
    }

    /**
     * If true, all datasets will be "normalized" to [0,1] scores.
     * @param normalize
     */
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    public List<Dataset> getAllInLanguage(Language lang) throws DaoException {
        List<Dataset> result = new ArrayList<Dataset>();
        for (Info i : info) {
            if (i.getLanguages().containsLanguage(lang)) {
                result.add(get(lang, i.getName()));
            }
        }
        return result;
    }

    /**
     * Reads a dataset from the classpath with a particular name.
     * Some datasets support multiple languages (i.e. simple and en).
     *
     * @param language The desired language
     * @param path The path to the dataset.
     * @return The dataset
     * @throws DaoException
     */
    public Dataset read(Language language, File path) throws DaoException {
        try {
            return read(path.getName(), language, WpIOUtils.openBufferedReader(path));
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Reads a dataset from the classpath with a particular name.
     * Some datasets support multiple languages (i.e. simple and en).
     *
     * @param language The desired language
     * @param name The name of the dataset.
     * @return The dataset
     * @throws DaoException
     */
    public Dataset get(Language language, String name) throws DaoException {
        if (name.contains("/") || name.contains("\\")) {
            throw new DaoException("get() reads a dataset by name for a jar. Try read() instead?");
        }
        Info info = getInfo(name);
        if (info == null) {
            throw new DaoException("no dataset with name '" + name + "'");
        }
        if (!info.languages.containsLanguage(language)) {
            throw new DaoException("dataset '" + name + "' does not support language " + language);
        }
        try {
            return read(name, language, WpIOUtils.openResource(RESOURCE_DATSET + "/" + name));
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Reads a dataset from the classpath with a particular name.
     * Some datasets support multiple languages (i.e. simple and en), this uses the default configuration.
     *
     * @param name The name of the dataset.
     * @return The dataset
     * @throws DaoException
     */
    public Dataset get(String name) throws DaoException {
        Info info = getInfo(name);
        if (info == null) {
            throw new DaoException("no dataset with name '" + name + "'");
        }
        return get(info.languages.getDefaultLanguage(), name);
    }

    /**
     * @param name
     * @return Returns information about the dataset with the specified name.
     */
    public Info getInfo(String name) {
        for (Info info : this.info) {
            if (info.name.equalsIgnoreCase(name)) {
                return info;
            }
        }
        return null;
    }

    /**
     * Sets the internal disambiguator AND marks resolve phrases to true.
     * @param dab
     */
    public void setDisambiguator(Disambiguator dab) {
        this.disambiguator = dab;
        this.resolvePhrases = true;
    }

    /**
     * @param resolvePhrases If true, phrases are resolved to local page ids
     *                   The disambiguator MUST be set as well.
     */
    public void setResolvePhrases(boolean resolvePhrases) {
        this.resolvePhrases = resolvePhrases;
        if (resolvePhrases && disambiguator == null) {
            throw new IllegalStateException("resolve phrases et to true, but no disambiguator specified.");
        }
    }

    /**
     * Reads a dataset from a buffered reader.
     * @param name Name of the dataset, must end with csv for comma separated files.
     * @param language Language of the dataset.
     * @param reader The inputsource of the dataset.
     * @return The dataset
     * @throws DaoException
     */
    protected Dataset read(String name, Language language, BufferedReader reader) throws DaoException {
        List<KnownSim> result = new ArrayList<KnownSim>();
        try {
            String delim = "\t";
            if (name.toLowerCase().endsWith("csv")) {
                delim = ",";
            }
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                String tokens[] = line.split(delim);
                if (tokens.length == 3) {
                    KnownSim ks = new KnownSim(
                                            tokens[0],
                                            tokens[1],
                                            Double.valueOf(tokens[2]),
                                            language
                                    );
                    if (resolvePhrases) {
                        LocalId id1 = disambiguator.disambiguateTop(new LocalString(language, ks.phrase1), null);
                        LocalId id2 = disambiguator.disambiguateTop(new LocalString(language, ks.phrase2), null);
                        if (id1 != null) { ks.wpId1 = id1.getId(); }
                        if (id2 != null) { ks.wpId2 = id2.getId(); }
                    }
                    result.add(ks);
                } else {
                    throw new DaoException("Invalid line in dataset file " + name + ": " +
                            "'" + StringEscapeUtils.escapeJava(line) + "'");
                }
            }
            reader.close();

        } catch (IOException e) {
            throw new DaoException(e);
        }
        Dataset dataset = new Dataset(name, language, result);
        if (normalize) {
            dataset.normalize();
        }
        return dataset;
    }

    /**
     * Writes a dataset out to a particular path
     * @param dataset
     * @param path
     * @throws DaoException
     */
    public void write(Dataset dataset, File path) throws DaoException {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            String delim = "\t";
            for (KnownSim ks: dataset.getData()) {
                writer.write(ks.phrase1 + delim + ks.phrase2 + delim + ks.similarity + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Read the embedded info.tsv file in the classpath.
     * @return
     * @throws DaoException
     */
    public static Collection<Info> readInfos() throws DaoException {
        try {
            return readInfos(WpIOUtils.openResource(RESOURCE_DATASET_INFO));
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Returns information about datasets in a reader.
     * @param reader
     * @return
     * @throws DaoException
     */
    public static Collection<Info> readInfos(BufferedReader reader) throws DaoException {
        try {
            List<Info> infos = new ArrayList<Info>();
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    String tokens[] = line.trim().split("\t");
                    infos.add(new Info(tokens[0], new LanguageSet(tokens[1])));
                } catch (IOException e) {
                    throw new DaoException(e);
                }
            }
            return infos;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
    public static class Provider extends org.wikapidia.conf.Provider<DatasetDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<DatasetDao> getType() {
            return DatasetDao.class;
        }

        @Override
        public String getPath() {
            return "sr.dataset.dao";
        }

        @Override
        public DatasetDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("resource")) {
                return null;
            }
            DatasetDao dao = new DatasetDao();
            if (config.hasPath("normalize")) {
                dao.setNormalize(config.getBoolean("normalize"));
            }
            if (config.hasPath("disambig")) {
                dao.setDisambiguator(
                        getConfigurator().get(Disambiguator.class, config.getString("disambig")));
            }
            if (config.hasPath("resolvePhrases")) {
                dao.setResolvePhrases(config.getBoolean("resolvePhrases"));
            }

            return dao;
        }
    }
}
