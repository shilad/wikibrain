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
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.dataset.DatasetDao;
import org.wikapidia.sr.ensemble.EnsembleMetric;
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
    private MonolingualSRMetric metric = null;
    private boolean deleteExistingModels = true;

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
    private boolean rebuildSubmetrics = true;

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

    public synchronized MonolingualSRMetric getMetric() throws ConfigurationException {
        if (metric == null) {
            this.metric = env.getConfigurator().get(MonolingualSRMetric.class, this.metricName, "language", language.getLangCode());
        }
        return metric;
    }

    public void build() throws ConfigurationException, DaoException, IOException, WikapidiaException {
        if (deleteExistingModels) {
            deleteExisting();
        }
        LOG.info("building metric " + metricName);
        String type = getMetricType();
        if (type.equals("ensemble")) {
            buildEnsemble();
        } else if (type.equals("pairwisecosinesim")) {
            buildCosineSim();
        } else {
            buildSimpleMetric();
        }
    }

    /**
     * This method takes care to not load the metric itself, and just deal in names.
     * Once the metric is loaded, it has already accessed its data files.
     * @throws ConfigurationException
     */
    public void deleteExisting() throws ConfigurationException {
        deleteMetricDir(metricName);
        if (getMetricType().equals("ensemble")) {
            for (String name : getMetricConfig().getStringList("metrics")) {
                deleteMetricDir(name);
            }
        }
        LOG.info("ALL DATA DIRECTORIES DELETED!");
    }

    public void buildSimpleMetric() throws ConfigurationException, DaoException, WikapidiaException, IOException {
        Dataset ds = getDataset();
        if (buildCosimilarity) {
            getMetric().writeMostSimilarCache(maxResults, rowIds, colIds);
        }
        getMetric().trainSimilarity(ds);
        getMetric().trainMostSimilar(ds, maxResults, null);
        getMetric().write();
    }

    public Dataset getDataset() throws ConfigurationException, DaoException {
        DatasetDao dao = env.getConfigurator().get(DatasetDao.class);
        List<Dataset> datasets = new ArrayList<Dataset>();
        for (String name : datasetNames) {
            datasets.add(dao.get(language, name));  // throws a DaoException if language is incorrect.
        }
        return new Dataset(datasets);   // merge all datasets together into one.
    }

    public void buildEnsemble() throws ConfigurationException, DaoException, WikapidiaException, IOException {
        EnsembleMetric ensemble = (EnsembleMetric) getMetric();
        Dataset ds = getDataset();
        ensemble.setTrainSubmetrics(false);         // Do it by hand

        // build up submetrics
        for (MonolingualSRMetric m : ensemble.getMetrics()) {
            if (buildCosimilarity && (rebuildSubmetrics || !m.hasMostSimilarCache())) {
                m.writeMostSimilarCache(maxResults * EnsembleMetric.EXTRA_SEARCH_DEPTH, rowIds, colIds);
            }
            if (rebuildSubmetrics || !m.mostSimilarIsTrained()) {
                m.trainMostSimilar(ds, maxResults * EnsembleMetric.EXTRA_SEARCH_DEPTH, null);
            }
            if (rebuildSubmetrics || !m.similarityIsTrained()) {
                m.trainSimilarity(ds);
            }
            m.write();
        }

        // Train can cascade to base metrics
        getMetric().trainSimilarity(ds);
        getMetric().trainMostSimilar(ds, maxResults, null);
        getMetric().writeMostSimilarCache(maxResults, rowIds, colIds);
        getMetric().write();
    }

    public void deleteMetricDir(String name) {
        File dir = FileUtils.getFile(srDir, name, language.getLangCode());
        FileUtils.deleteQuietly(dir);
    }

    public void buildCosineSim() {
        throw new UnsupportedOperationException();
    }

    public String getMetricType() throws ConfigurationException {
        return getMetricConfig().getString("type");
    }

    public Config getMetricConfig() throws ConfigurationException {
        return env.getConfigurator().getConfig(MonolingualSRMetric.class, metricName);
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

    public void setDeleteExistingModels(boolean deleteExistingModels) {
        this.deleteExistingModels = deleteExistingModels;
    }

    public void setRebuildSubmetrics(boolean rebuildSubmetrics) {
        this.rebuildSubmetrics = rebuildSubmetrics;
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
                        .withDescription("delete existing models (true or false, default is true)")
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

        // when building pairwise cosine and ensembles, don't rebuild already built sub-metrics.
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("skip-submetrics")
                        .withDescription("For ensemble and pairwise cosine, don't build already built submetrics (implies -d false)")
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
            builder.setRebuildSubmetrics(false);
            builder.setDeleteExistingModels(false);
        }
        if (cmd.hasOption("d")) {
            builder.setDeleteExistingModels(Boolean.valueOf(cmd.getOptionValue("d")));
        }

        builder.build();
    }
}
