package org.wikapidia.sr.evaluation;

import com.typesafe.config.ConfigException;
import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;
import org.wikapidia.sr.utils.KnownSim;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ben Hillmann
 * @author Matt Lesicko
 */
public class CrossValidation {
    private static final int DEFAULT_SPLITS = 7;
    private int missing;
    private int failed;

    public CrossValidation(){
        missing=0;
        failed=0;
    }

    //Evaluation
    private double evaluate(LocalSRMetric srMetric, Dataset dataset) throws DaoException {
        int size = dataset.getData().size();
        double[] estimate = new double[size];
        double[] real = new double[size];
        for (int i=0; i<size; i++){
            try {
                KnownSim knownSim = dataset.getData().get(i);
                SRResult result = srMetric.similarity(knownSim.phrase1, knownSim.phrase2, knownSim.language, false);
                if (Double.isNaN(result.getScore())){
                    missing++;
                }
                estimate[i]=result.getScore();
                real[i]=knownSim.similarity;
            } catch (Exception e){
                failed++;
            }

        }
        SpearmansCorrelation pearsonsCorrelation = new SpearmansCorrelation();
        return pearsonsCorrelation.correlation(estimate,real);
    }

    private double evaluate(UniversalSRMetric srMetric, Dataset dataset) throws DaoException {
        int size = dataset.getData().size();
        double[] estimate = new double[size];
        double[] real = new double[size];

        for (int i=0; i<size; i++){
            try {
                KnownSim knownSim = dataset.getData().get(i);
                LocalString phrase1 = new LocalString(knownSim.language, knownSim.phrase1);
                LocalString phrase2 = new LocalString(knownSim.language, knownSim.phrase2);
                SRResult result = srMetric.similarity(phrase1, phrase2, false);
                if (Double.isNaN(result.getScore())){
                    missing++;
                }
                estimate[i]=result.getScore();
                real[i]=knownSim.similarity;
            }
            catch (Exception e){
                failed++;
            }
        }
        SpearmansCorrelation pearsonsCorrelation = new SpearmansCorrelation();
        return pearsonsCorrelation.correlation(estimate,real);
    }

    public static void makeFolds(List<Dataset> splits, List<Dataset> trains, List<Dataset> tests) {
        for (int i = 0; i < splits.size(); i++) {
            tests.add(splits.get(i));
            // make copy of datasets without test set
            List<Dataset> splits2 = new ArrayList<Dataset>(splits);
            splits2.remove(i);
            trains.add(new Dataset(splits2));
        }
    }

    public static void main(String[] args) throws DaoException, ConfigurationException {
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
                        .isRequired()
                        .withLongOpt("gold")
                        .withDescription("the set of gold standard datasets to train on, separated by commas")
                        .create("g"));

        //Specify the Metrics
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("metric")
                        .withDescription("set a local metric")
                        .create("m"));

        //Cross-validation mode
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("cross-validation-mode")
                        .withDescription("Set cross validation mode (none, within-dataset, between-dataset)")
                        .create("x"));

        //Specify the Folds
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("folds")
                        .withDescription("set the number of folds to evaluate on" )
                        .create("k"));


        Env.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("MetricTrainer", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator c = env.getConfigurator();


        DatasetDao datasetDao = new DatasetDao();

        if (!cmd.hasOption("u")&&!cmd.hasOption("m")){
            throw new IllegalArgumentException("Must specify a metric to evaluate.");
        }

        int k =  cmd.hasOption("k")
                ? Integer.parseInt(cmd.getOptionValue("k" ))
                : DEFAULT_SPLITS;

        // TODO: figure out interaction with "-d"
        // TODO: display error if neither "-d" or "-g" are specified
        // TODO: handle multiple languages
        File datasetPath = new File(c.getConf().get().getString("sr.dataset.path"));
        LanguageSet validLanguages = env.getLanguages();
        List<Dataset> datasets = new ArrayList<Dataset>();
        for (String dsName : cmd.getOptionValues("g")) {
            boolean foundOne = false;
            //Check if it's a known dataset
            try {
                List<String> languages = c.getConf().get().getStringList("sr.dataset.sets."+dsName);
                for (String langCode : languages){
                    Language lang = Language.getByLangCode(langCode);
                    if (validLanguages==null||validLanguages.containsLanguage(lang)){
                        datasets.add(datasetDao.read(lang,new File(datasetPath, dsName).getAbsolutePath()));
                    }
                }
            }catch (ConfigException.Missing e){
                //Check if it's a stored dataset
                for (Language lang : validLanguages){
                    try {
                        for (int i=0; i<k; i++){
                            String name = lang.getLangCode()+"-"+dsName+"-"+i+"of"+k+".txt";
                            datasets.add(datasetDao.read(lang, new File(datasetPath, name).getAbsolutePath()));
                        }
                        foundOne = true;
                    }
                    catch (DaoException f){}
                }
                if (!foundOne){
                    throw new IllegalArgumentException("Could not find valid dataset "+dsName+" in languages "+validLanguages.getLangCodes().toString());
                }
            }
        }

        List<Dataset> allTrain = new ArrayList<Dataset>();
        List<Dataset> allTest = new ArrayList<Dataset>();
        String mode = cmd.hasOption("x") ? cmd.getOptionValue("x") : "within-dataset";

        if (mode.equals("none")) {
            for (Dataset ds : datasets) {
                allTrain.add(ds);
                allTest.add(ds);
            }
        } else if (mode.equals("within-dataset")) {
            for (Dataset ds : datasets) {
                makeFolds(ds.split(k), allTrain, allTest);
            }
        } else if (mode.equals("across-dataset")) {
            makeFolds(datasets, allTrain, allTest);
        } else {
            System.err.println("Unknown mode: " + mode);
            System.exit(1);
        }

        double sumError = 0;
        CrossValidation crossValidation = new CrossValidation();

        for (int i = 0; i < allTrain.size(); i++) {
            Dataset train = allTrain.get(i);
            Dataset test = allTest.get(i);

            LocalSRMetric sr = null;
            UniversalSRMetric usr = null;
            if (cmd.hasOption("m")){
                sr = c.get(LocalSRMetric.class,cmd.getOptionValue("m"));
            }
            if (cmd.hasOption("u")){
                usr = c.get(UniversalSRMetric.class,cmd.getOptionValue("u"));
            }

            if (sr!=null){
                sr.trainDefaultSimilarity(train);
                sr.trainSimilarity(train);
                sumError+=crossValidation.evaluate(sr, test);
            } else if (usr!=null){
                usr.trainSimilarity(train);
                sumError+=crossValidation.evaluate(usr, test);
            }
        }
        System.out.println(sumError / allTrain.size());
        System.out.println(crossValidation.missing+" missing and "+crossValidation.failed+" failed");
    }

}