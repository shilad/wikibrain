package org.wikibrain.sr.dataset;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.utils.KnownSim;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(Dataset.class);

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
     * The dataset name can also be a group name (e.g. en-major)
     *
     * @param language The desired language
     * @param name The name of the dataset.
     * @return The dataset
     * @throws DaoException
     */
    public Dataset get(Language language, String name) throws DaoException {
        if (groups.containsKey(name)) {
            List<Dataset> members = new ArrayList<Dataset>();
            for (String n : groups.get(name)) {
                members.add(get(language, n));
            }
            return new Dataset(name, members);
        }
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
     * Returns true if the name is the name of a group of datasets
     * @param name
     * @return
     */
    public boolean isGroup(String name) {
        return groups.containsKey(name);
    }

    /**
     * Return all the member datasets in the specified group.
     * @param language
     * @param name
     * @return
     * @throws DaoException
     */
    public List<Dataset> getGroup(Language language, String name) throws DaoException {
        List<Dataset> members = new ArrayList<Dataset>();
        for (String n : groups.get(name)) {
            members.add(get(language, n));
        }
        return members;
    }

    public List<Dataset> getDatasetOrGroup(Language language, String name) throws DaoException {
        if (isGroup(name)) {
            return getGroup(language, name);
        } else {
            return Arrays.asList(get(language, name));
        }
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

    public void setGroups(Map<String, List<String>> groups) {
        this.groups = groups;
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
                if (tokens.length >= 3) {
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
    public static class Provider extends org.wikibrain.conf.Provider<DatasetDao> {
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
            Map<String, List<String>> groups = new HashMap<String, List<String>>();
            Config groupConfig = getConfig().get().getConfig("sr.dataset.groups");
            for (Map.Entry<String, ConfigValue> entry  : groupConfig.entrySet()) {
                groups.put(entry.getKey(), (List<String>)entry.getValue().unwrapped());
            }
            dao.setGroups(groups);

            return dao;
        }
    }
}
