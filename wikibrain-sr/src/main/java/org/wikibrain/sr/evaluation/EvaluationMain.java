package org.wikibrain.sr.evaluation;

import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class EvaluationMain {
    private static final int DEFAULT_FOLDS = 7;

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikiBrainException {

        Options options = new Options();

        //Specify for universal metric
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("universal")
                        .withDescription("set a universal metric")
                        .create("u"));

        //Specify the Dataset
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("gold")
                        .withDescription("the set of gold standard datasets to train on")
                        .create("g"));

        // Use an existing pre-trained metric.
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("pretrained")
                        .withDescription("use an existing pretrained metric")
                        .create("a"));

        //Specify the Metrics
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("metric")
                        .withDescription("set a local metric")
                        .create("m"));

        //specify the output directory
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("outputDir")
                        .withDescription("Specify the output directory")
                        .create("o"));

        //reload a saved dataset
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("reload")
                        .withDescription("reload a previously stored split dataset")
                        .create("r"));

        //Cross-validation mode
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("cross-validation-mode")
                        .withDescription("Set cross validation mode (none, within-dataset, between-dataset)")
                        .create("x"));

        // Prediction mode
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("prediction-mode")
                        .withDescription("Set prediction mode (similarity, mostsimilar)")
                        .create("p"));
        //Specify the Folds
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("folds")
                        .withDescription("set the number of folds to evaluate on")
                        .create("k"));

        //Resolve phrases to ids
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("resolve")
                        .withDescription("resolve phrases to ids")
                        .create("v"));

        //Resolve phrases to ids
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("buildMostSimilarCache")
                        .withDescription("build most similar cache matrices")
                        .create("z"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("MetricTrainer", options);
            System.exit(1);
            return; // to appease the compiler
        }

        Env env = new EnvBuilder(cmd)
                .setProperty("sr.metric.training", !cmd.hasOption("a"))
                .build();
        Configurator c = env.getConfigurator();
        DatasetDao dsDao = c.get(DatasetDao.class);
        int folds =  cmd.hasOption("k")
                ? Integer.parseInt(cmd.getOptionValue("k" ))
                : DEFAULT_FOLDS;

        if (cmd.hasOption("u")) { // TODO: support universal evaluations
            throw new UnsupportedOperationException();
        }

        if (!cmd.hasOption("u") && !cmd.hasOption("m")){
            System.err.println("Must specify a metric to evaluate.");
            new HelpFormatter().printHelp("MetricTrainer", options);
            System.exit(1);
            return; // to appease the compiler
        }

        if (cmd.hasOption("u") && cmd.hasOption("m")){
            System.err.println("Can only operate on one metric at a time");
            new HelpFormatter().printHelp("MetricTrainer", options);
            System.exit(1);
            return;
        }
        if (cmd.hasOption("r")){
            throw new UnsupportedOperationException();
        } else if (!cmd.hasOption("g")){
            System.err.println("Must specify a dataset using either -g or -r");
            new HelpFormatter().printHelp("MetricTrainer", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("u")) {
            throw new UnsupportedOperationException();  // TODO: implement universal metrics
        }
        Language lang = env.getLanguages().getDefaultLanguage();
        List<Dataset> datasets = new ArrayList<Dataset>();
        String mode = cmd.hasOption("x") ? cmd.getOptionValue("x") : "within-dataset";
        for (String dsName : cmd.getOptionValues("g")) {
            datasets.addAll(dsDao.getDatasetOrGroup(lang, dsName));
        }

        String outputDir = cmd.hasOption("o")
                ? cmd.getOptionValue("o")
                : c.getConf().get().getString("sr.dataset.records");

        Evaluator evaluator;
        if (!cmd.hasOption("p") || cmd.getOptionValue("p").equals("similarity")) {
            evaluator = new SimilarityEvaluator(new File(outputDir));
        } else if (cmd.getOptionValue("p").equals("mostsimilar")) {
            evaluator = new MostSimilarEvaluator(new File(outputDir));
            if (cmd.hasOption("z")) {
                ((MostSimilarEvaluator)evaluator).setBuildCosimilarityMatrix(true);
            }
        } else {
            System.err.println("Invalid prediction mode. usage:");
            new HelpFormatter().printHelp("MetricTrainer", options);
            System.exit(1);
            return; // to appease the compiler
        }

        if (cmd.hasOption("v")) {
            evaluator.setResolvePhrases(true);
        }

        if (mode.equals("none")) {
            Dataset all = new Dataset(datasets);
            evaluator.addSplit(new Split(all.getName(), all.getName(), all, all));
        } else if (mode.equals("within-dataset")) {
            for (Dataset ds : datasets) {
                evaluator.addCrossfolds(ds, folds);
            }
        } else if (mode.equals("across-dataset")) {
            evaluator.addCrossfolds(new Dataset(datasets), folds);
        } else {
            System.err.println("Unknown mode: " + mode);
            System.exit(1);
        }

        MonolingualSRFactory factory;
        if (cmd.hasOption("a")) {
            SRMetric sr = env.getConfigurator().get(
                                        SRMetric.class,
                                        cmd.getOptionValue("m"),
                                        "language",
                                        lang.getLangCode()
                                    );
            factory = new PretrainedSRFactory(sr);
        } else {
            factory = new ConfigMonolingualSRFactory(
                    lang, env.getConfigurator(), cmd.getOptionValue("m"));

        }
        evaluator.evaluate(factory);
    }
}
