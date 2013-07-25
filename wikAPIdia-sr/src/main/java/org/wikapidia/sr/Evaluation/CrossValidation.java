package org.wikapidia.sr.Evaluation;

import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;
import org.wikapidia.sr.utils.KnownSim;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ben Hillmann
 * @author Matt Lesicko
 */
public class CrossValidation {

    //Evaluation
    private static double evaluate(LocalSRMetric srMetric, Dataset dataset) throws DaoException {
        int size = dataset.getData().size();
        double[] estimate = new double[size];
        double[] real = new double[size];

        int missing = 0;
        int failed = 0;
        for (int i=0; i<size; i++){
            try {
                KnownSim knownSim = dataset.getData().get(i);
                SRResult result = srMetric.similarity(knownSim.phrase1, knownSim.phrase2, knownSim.language, false);
                if (Double.isNaN(result.getValue())){
                    missing++;
                }
                estimate[i]=result.getValue();
                real[i]=knownSim.similarity;
            } catch (Exception e){
                failed++;
            }

        }
        System.out.println(missing+" missing and "+failed+" failed");
        SpearmansCorrelation pearsonsCorrelation = new SpearmansCorrelation();
        return pearsonsCorrelation.correlation(estimate,real);
    }

    private static double evaluate(UniversalSRMetric srMetric, Dataset dataset) throws DaoException {
        int size = dataset.getData().size();
        double[] estimate = new double[size];
        double[] real = new double[size];

        int missing = 0;
        int failed = 0;
        for (int i=0; i<size; i++){
            try {
                KnownSim knownSim = dataset.getData().get(i);
                LocalString phrase1 = new LocalString(knownSim.language, knownSim.phrase1);
                LocalString phrase2 = new LocalString(knownSim.language, knownSim.phrase2);
                SRResult result = srMetric.similarity(phrase1, phrase2, false);
                if (Double.isNaN(result.getValue())){
                    missing++;
                }
                estimate[i]=result.getValue();
                real[i]=knownSim.similarity;
            }
            catch (Exception e){
                failed++;
            }
        }
        System.out.println(missing+" missing and "+failed+" failed");
        SpearmansCorrelation pearsonsCorrelation = new SpearmansCorrelation();
        return pearsonsCorrelation.correlation(estimate,real);
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
        //Specify the Folds
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("folds" )
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

        int k = Integer.parseInt(cmd.getOptionValue("k" ));

        List<Dataset> datasets = new ArrayList<Dataset>();
        DatasetDao datasetDao = new DatasetDao();
        List<String> datasetConfig = c.getConf().get().getStringList("sr.dataset.names");
        String datasetPath = c.getConf().get().getString("sr.dataset.path");
        if (cmd.hasOption("g")) {
            String[] datasetNames = cmd.getOptionValues("g");
            for (String name : datasetNames){
                if (datasetConfig.contains(name)){
                    int langPosition = datasetConfig.indexOf(name)-1;
                    Language language = Language.getByLangCode(datasetConfig.get(langPosition));
                    datasets.add(datasetDao.read(language,datasetPath+name));
                }
                else {
                    throw new IllegalArgumentException("Specified dataset "+name+" is not in the configuration file.");
                }
            }
        } else {
            for (int i = 0; i < datasetConfig.size();i+=2) {
                String language = datasetConfig.get(i);
                String datasetName = datasetConfig.get(i+1);
                datasets.add(datasetDao.read(Language.getByLangCode(language), datasetPath + datasetName));
            }
        }
        if (cmd.hasOption("d")){
            String name = cmd.getOptionValue("d");
            Dataset combinedDataset = new Dataset(datasets);
            datasets = combinedDataset.split(k);
            for (int i=0; i<k; i++){
                datasetDao.write(datasets.get(i),datasetPath+name+"-"+(i+1)+"of"+k+".txt");
            }

        }

        double sumError = 0;


        for (Dataset testSet : datasets){
            LocalSRMetric sr = null;
            UniversalSRMetric usr = null;
            if (cmd.hasOption("m")){
                sr = c.get(LocalSRMetric.class,cmd.getOptionValue("m"));
            }
            if (cmd.hasOption("u")){
                usr = c.get(UniversalSRMetric.class,cmd.getOptionValue("u"));
            }

            if (sr!=null){
                for (Dataset trainingSet : datasets){
                    if (trainingSet!=testSet){
                        sr.trainSimilarity(trainingSet);
                    }
                }
                sumError+=evaluate(sr,testSet);
            } else if (usr!=null){
                for (Dataset trainingSet : datasets){
                    if (trainingSet!=testSet){
                        usr.trainSimilarity(trainingSet);
                    }
                }
                sumError+=evaluate(usr,testSet);
            }
        }
        System.out.println(sumError/k);
    }

}