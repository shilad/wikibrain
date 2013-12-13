package org.wikapidia.sr.evaluation;

import org.apache.commons.cli.*;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.dataset.DatasetDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class EvaluationMain {
    private static final int DEFAULT_FOLDS = 7;

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikapidiaException {

        Options options = new Options();
        //Specify whether you have the split datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("datasets" )
                        .withDescription("drop and create the split datasets with given name" )
                        .create("d" ));

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
                .setProperty("sr.metric.training", true)
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

        Language lang = env.getLanguages().getDefaultLanguage();
        List<Dataset> datasets = new ArrayList<Dataset>();
        String mode = cmd.hasOption("x") ? cmd.getOptionValue("x") : "within-dataset";
        for (String dsName : cmd.getOptionValues("g")) {
            datasets.add(dsDao.get(lang, dsName));
        }

        String outputDir = cmd.hasOption("o")
                ? cmd.getOptionValue("o")
                : c.getConf().get().getString("sr.dataset.records");

        Evaluator evaluator;
        if (!cmd.hasOption("p") || cmd.getOptionValue("p").equals("similarity")) {
            evaluator = new SimilarityEvaluator(new File(outputDir));
        } else if (cmd.getOptionValue("p").equals("mostsimilar")) {
            evaluator = new MostSimilarEvaluator(new File(outputDir));
        } else {
            System.err.println("Invalid prediction mode. usage:");
            new HelpFormatter().printHelp("MetricTrainer", options);
            System.exit(1);
            return; // to appease the compiler
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

        if (cmd.hasOption("m")) {
            LocalSRFactory factory = new ConfigLocalSRFactory(
                    env.getConfigurator(), cmd.getOptionValue("m"));
            evaluator.evaluate(factory);
        } else if (cmd.hasOption("u")) {
            throw new UnsupportedOperationException();  // TODO: implement universal metrics
        }
    }
}
