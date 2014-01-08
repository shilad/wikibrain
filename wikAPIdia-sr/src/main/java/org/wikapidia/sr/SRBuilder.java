package org.wikapidia.sr;

import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.dataset.DatasetDao;
import org.wikapidia.sr.ensemble.EnsembleMetric;
import org.wikapidia.sr.esa.SRConceptSpaceGenerator;
import org.wikapidia.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * A "script" to build the semantic relatedness models.
 * This script takes care to not load the metric in build() until after data directories are deleted.
 *
 * @author Shilad Sen
 */
public class SRBuilder {
    private static final Logger LOG = Logger.getLogger(SRBuilder.class.getName());

    // The environment and configuration we will use.
    private final Env env;
    private final Configuration config;
    private final Language language;
    private final File srDir;

    // The name of the metric we will use.
    // If null, corresponds to the configured default metric.
    private String metricName = null;
    private boolean deleteExistingData = true;

    // The maximum number of results
    private int maxResults = 500;

    // Information that corresponds to building the cosimilarity matrix
    private boolean buildCosimilarity = false;
    private TIntSet rowIds = null;
    private TIntSet colIds = null;

    // List of datasets that will be used
    private List<String> datasetNames;

    // If false, existing submetrics for ensemble and pairwsise sim that
    // are already built will not be rebuilt.
    private boolean skipBuiltMetrics = false;

    public static enum Mode {
        SIMILARITY,
        MOSTSIMILAR,
        BOTH
    }

    private Mode mode = Mode.BOTH;


    public SRBuilder(Env env, String metricName) throws ConfigurationException {
        this.env = env;
        this.language = env.getLanguages().getDefaultLanguage();
        this.config = env.getConfiguration();
        this.srDir = new File(config.get().getString("sr.metric.path"));
        datasetNames = config.get().getStringList("sr.dataset.defaultsets");

        // Properly resolve the default metric name.
        this.metricName = env.getConfigurator().resolveComponentName(MonolingualSRMetric.class, metricName);
        if (!srDir.isDirectory()) {
            srDir.mkdirs();
        }
    }

    public synchronized  MonolingualSRMetric getMetric() throws ConfigurationException {
        return getMetric(metricName);
    }

    public synchronized MonolingualSRMetric getMetric(String name) throws ConfigurationException {
            return env.getConfigurator().get(MonolingualSRMetric.class, name, "language", language.getLangCode());
    }

    /**
     * First deletes models if deleteExistingData is true, then builds the appropriate metrics.
     * @throws ConfigurationException
     * @throws DaoException
     * @throws IOException
     * @throws WikapidiaException
     */
    public void build() throws ConfigurationException, DaoException, IOException, WikapidiaException {
        if (deleteExistingData) {
            deleteDataDirectories();
        }
        buildConceptsIfNecessary();
        LOG.info("building metric " + metricName);
        String type = getMetricType();
        if (type.equals("ensemble")) {
            initEnsemble();
        } else if (type.equals("pairwisecosinesim")) {
            initCosineSim();
        } else {
            initSimpleMetric();
        }
        for (String name : getSubmetrics(metricName)) {
            buildMetric(name);
        }
    }

    /**
     * This method takes care to not load the metric itself, and just deal in names.
     * Once the metric is loaded, it has already accessed its data files.
     * @throws ConfigurationException
     */
    public void deleteDataDirectories() throws ConfigurationException {
        for (String name : getSubmetrics(metricName)) {
            File dir = FileUtils.getFile(srDir, name, language.getLangCode());
            if (dir.exists()) {
                LOG.info("deleting metric directory " + dir);
                FileUtils.deleteQuietly(dir);
            }
        }
    }

    /**
     * Returns a list of metric names (including the passed in name) that are a submetric
     * of the specified metric. The metrics are topologically sorted by dependency, so the
     * parent metric will appear last.
     *
     * @param parentName
     * @return
     * @throws ConfigurationException
     */
    public List<String> getSubmetrics(String parentName) throws ConfigurationException {
        List<String> results = new ArrayList<String>();
        String type = getMetricType(parentName);
        Config config = getMetricConfig(parentName);
        if (type.equals("ensemble")) {
            for (String child : config.getStringList("metrics")) {
                results.addAll(getSubmetrics(child));
                results.add(child);
            }
        } else if (type.equals("vector.mostsimilarconcepts")) {
            results.addAll(getSubmetrics(config.getString("generator.basemetric")));
        }
        results.add(parentName);
        return results;
    }

    public void initSimpleMetric() throws ConfigurationException, DaoException, WikapidiaException, IOException {
        // nothing necessary
    }

    public void initEnsemble() throws ConfigurationException, DaoException, WikapidiaException, IOException {
        EnsembleMetric ensemble = (EnsembleMetric) getMetric();
        ensemble.setTrainSubmetrics(false);         // Do it by hand
    }

    public void initCosineSim() {
        // nothing, for now.
    }

    public void buildMetric(String name) throws ConfigurationException, DaoException, IOException {
        LOG.info("building component metric " + name);
        Dataset ds = getDataset();
        MonolingualSRMetric metric = getMetric(name);
        if (metric instanceof BaseMonolingualSRMetric) {
            ((BaseMonolingualSRMetric)metric).setBuildMostSimilarCache(buildCosimilarity);
        }
        if (mode == Mode.SIMILARITY || mode == Mode.BOTH) {
            if (skipBuiltMetrics && metric.similarityIsTrained()) {
                LOG.info("metric " + name + " similarity() is already trained... skipping");
            } else {
                metric.trainSimilarity(ds);
            }
        }

        if (mode == Mode.MOSTSIMILAR || mode == Mode.BOTH) {
            if (skipBuiltMetrics && metric.mostSimilarIsTrained()) {
                LOG.info("metric " + name + " mostSimilar() is already trained... skipping");
            } else {
                metric.trainMostSimilar(ds, maxResults * EnsembleMetric.EXTRA_SEARCH_DEPTH, null);
            }
        }
        metric.write();
    }

    private void buildConceptsIfNecessary() throws IOException, ConfigurationException, DaoException {
        boolean needsConcepts = false;
        for (String name : getSubmetrics(metricName)) {
            String type = getMetricType(name);
            if (type.equals("vector.esa") || type.equals("vector.mostsimilarconcepts")) {
                needsConcepts = true;
            }
        }
        if (!needsConcepts) {
            return;
        }
        File path = FileUtils.getFile(
                    env.getConfiguration().get().getString("sr.concepts.path"),
                    language.getLangCode() + ".txt"
               );
        path.getParentFile().mkdirs();

        // Check to see if concepts are already built
        if (path.isFile() && FileUtils.readLines(path).size() > 1) {
            return;
        }

        LOG.info("building concept file " + path.getAbsolutePath() + " for " + metricName);
        SRConceptSpaceGenerator gen = new SRConceptSpaceGenerator(language,
                env.getConfigurator().get(LocalLinkDao.class),
                env.getConfigurator().get(LocalPageDao.class));
        gen.writeConcepts(path);
        LOG.info("finished creating concept file " + path.getAbsolutePath() +
                " with " + FileUtils.readLines(path).size() + " lines");
    }

    public Dataset getDataset() throws ConfigurationException, DaoException {
        DatasetDao dao = env.getConfigurator().get(DatasetDao.class);
        List<Dataset> datasets = new ArrayList<Dataset>();
        for (String name : datasetNames) {
            datasets.add(dao.get(language, name));  // throws a DaoException if language is incorrect.
        }
        return new Dataset(datasets);   // merge all datasets together into one.
    }


    public String getMetricType() throws ConfigurationException {
        return getMetricType(metricName);
    }

    public String getMetricType(String name) throws ConfigurationException {
        Config config = getMetricConfig(name);
        String type = config.getString("type");
        if (type.equals("vector")) {
            type += "." + config.getString("generator.type");
        }
        return type;
    }

    public Config getMetricConfig() throws ConfigurationException {
        return getMetricConfig(metricName);
    }

    public Config getMetricConfig(String name) throws ConfigurationException {
        return env.getConfigurator().getConfig(MonolingualSRMetric.class, name);
    }

    public void setRowIdsFromFile(String path) throws IOException {
        rowIds = readIds(path);
    }

    public void setColIdsFromFile(String path) throws IOException {
        colIds = readIds(path);
    }

    public void setDatasetNames(List<String> datasetNames) {
        this.datasetNames = datasetNames;
    }

    public void setBuildCosimilarity(boolean buildCosimilarity) {
        this.buildCosimilarity = buildCosimilarity;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setRowIds(TIntSet rowIds) {
        this.rowIds = rowIds;
    }

    public void setColIds(TIntSet colIds) {
        this.colIds = colIds;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
    public void setDeleteExistingData(boolean deleteExistingData) {
        this.deleteExistingData = deleteExistingData;
    }

    public void setSkipBuiltMetrics(boolean skipBuiltMetrics) {
        this.skipBuiltMetrics = skipBuiltMetrics;
    }

    private static TIntSet readIds(String path) throws IOException {
        TIntSet ids = new TIntHashSet();
        BufferedReader reader = WpIOUtils.openBufferedReader(new File(path));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            ids.add(Integer.valueOf(line.trim()));
        }
        reader.close();
        return ids;
    }

    public static void main(String args[]) throws ConfigurationException, IOException, WikapidiaException, DaoException {
        Options options = new Options();

        //Number of Max Results(otherwise take from config)
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("max-results")
                        .withDescription("maximum number of results")
                        .create("r"));
        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("gold")
                        .withDescription("the set of gold standard datasets to train on")
                        .create("g"));

        //Delete existing data models
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("delete")
                        .withDescription("delete all existing SR data for the metric and its submetrics (true or false, default is true)")
                        .create("d"));

        //Specify the Metrics
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("metric")
                        .withDescription("set a local metric")
                        .create("m"));

        // Row and column ids for most similar caches
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("rowids")
                        .withDescription("page ids for rows of cosimilarity matrices (implies -s)")
                        .create("p"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("colids")
                        .withDescription("page ids for columns of cosimilarity matrices (implies -s)")
                        .create("q"));

        // build the cosimilarity matrix
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("cosimilarity")
                        .withDescription("build cosimilarity matrices")
                        .create("s"));

        // build the cosimilarity matrix
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("mode")
                        .withDescription("mode: similarity, mostsimilar, or both")
                        .create("p"));

        // when building pairwise cosine and ensembles, don't rebuild already built sub-metrics.
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("skip-built")
                        .withDescription("Don't rebuild already built bmetrics (implies -d false)")
                        .create("k"));

        EnvBuilder.addStandardOptions(options);


        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("SRBuilder", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        String metric = cmd.hasOption("m") ? cmd.getOptionValue("m") : null;
        SRBuilder builder = new SRBuilder(env, metric);
        if (cmd.hasOption("g")) {
            builder.setDatasetNames(Arrays.asList(cmd.getOptionValues("g")));
        }
        if (cmd.hasOption("p")) {
            builder.setRowIdsFromFile(cmd.getOptionValue("p"));
            builder.setBuildCosimilarity(true);
        }
        if (cmd.hasOption("q")) {
            builder.setColIdsFromFile(cmd.getOptionValue("q"));
            builder.setBuildCosimilarity(true);
        }
        if (cmd.hasOption("s")) {
            builder.setBuildCosimilarity(true);
        }
        if (cmd.hasOption("k")) {
            builder.setSkipBuiltMetrics(true);
            builder.setDeleteExistingData(false);
        }
        if (cmd.hasOption("d")) {
            builder.setDeleteExistingData(Boolean.valueOf(cmd.getOptionValue("d")));
        }
        if (cmd.hasOption("p")) {
            builder.setMode(Mode.valueOf(cmd.getOptionValue("p").toUpperCase()));
        }

        builder.build();
    }
}
