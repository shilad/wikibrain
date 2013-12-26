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
 * A "script" to build the semantic relatedness models
 *
 * @author Shilad Sen
 */
public class SRBuilder {
    private static final Logger LOG = Logger.getLogger(SRBuilder.class.getName());

    // The environment and configuration we will use.
    private final Env env;
    private final Configuration config;
    private final Language language;

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

    public SRBuilder(Env env, String metricName) throws ConfigurationException {
        this.env = env;
        this.language = env.getLanguages().getDefaultLanguage();
        this.config = env.getConfiguration();
        datasetNames = config.get().getStringList("sr.dataset.defaultsets");

        // Properly resolve the default metric name.
        this.metricName = env.getConfigurator().resolveComponentName(MonolingualSRMetric.class, metricName);
        this.metric = env.getConfigurator().get(MonolingualSRMetric.class, this.metricName, "language", language.getLangCode());
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

    public void deleteExisting() throws ConfigurationException {
        deleteMetricDir(metric.getName());
        if (getMetricType().equals("ensemble")) {
            EnsembleMetric ensemble = (EnsembleMetric)metric;
            for (MonolingualSRMetric m : ensemble.getMetrics()) {
                deleteMetricDir(m.getName());
            }
        }
    }

    public void buildSimpleMetric() throws ConfigurationException, DaoException, WikapidiaException, IOException {
        Dataset ds = getDataset();
        if (buildCosimilarity) {
            metric.writeCosimilarity(maxResults, rowIds, colIds);
        }
        metric.trainSimilarity(ds);
        metric.trainMostSimilar(ds,maxResults,null);
        metric.write();
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
        EnsembleMetric ensemble = (EnsembleMetric)metric;
        if (buildCosimilarity) {
            for (MonolingualSRMetric m : ensemble.getMetrics()) {
                m.writeCosimilarity(maxResults*EnsembleMetric.EXTRA_SEARCH_DEPTH, rowIds, colIds);
            }
            metric.writeCosimilarity(maxResults*EnsembleMetric.EXTRA_SEARCH_DEPTH, rowIds, colIds);
        }
        Dataset ds = getDataset();

        // Train cascades to base metrics
        metric.trainSimilarity(ds);
        metric.trainMostSimilar(ds,maxResults,null);
        for (MonolingualSRMetric m : ensemble.getMetrics()) {
            m.write();
        }
        metric.write();
    }

    public void deleteMetricDir(String name) {
        FileUtils.deleteQuietly(FileUtils.getFile(name, language.getLangCode()));
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
        rowIds = readIds(path);
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
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("cosimilarity")
                        .withDescription("build cosimilarity matrices")
                        .create("s"));

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
        if (cmd.hasOption("d")) {
            builder.setDeleteExistingModels(Boolean.valueOf(cmd.getOptionValue("d")));
        }

        builder.build();
    }
}
