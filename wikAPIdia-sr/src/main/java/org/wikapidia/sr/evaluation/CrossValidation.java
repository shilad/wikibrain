package org.wikapidia.sr.evaluation;

import com.typesafe.config.Config;
import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;
import org.wikapidia.sr.utils.KnownSim;

import java.io.*;
import java.util.*;

/**
 * @author Ben Hillmann
 * @author Matt Lesicko
 */
public class CrossValidation {
    private static final int DEFAULT_SPLITS = 7;
    private static final String SUMMARY_HEADER = "language\tsr\tdisambiguator\tdataset\tmode\tpearsons\tspearmans\tmissing\tfailed\tsr details\trun details file\n";
    private int missing;
    private int failed;
    private String fileText;
    private final Date startDate;
    private List<Double> pearsonScores;
    private List<Double> spearmanScores;

    public CrossValidation(){
        missing=0;
        failed=0;
        startDate=new Date();
        pearsonScores = new ArrayList<Double>();
        spearmanScores = new ArrayList<Double>();
        fileText="language\tterm1\tterm2\tgold standard value\tpredicted value\n";
    }

    //Evaluation
    private void evaluate(LocalSRMetric srMetric, Dataset dataset) throws DaoException {
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
                fileText+=knownSim.language+"\t"+knownSim.phrase1+"\t"+knownSim.phrase2+"\t"+knownSim.similarity+"\t"+result.getScore()+"\n";
                estimate[i]=result.getScore();
                real[i]=knownSim.similarity;
            } catch (Exception e){
                failed++;
            }

        }

        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();
        SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
        pearsonScores.add(pearsonsCorrelation.correlation(estimate, real));
        spearmanScores.add(spearmansCorrelation.correlation(estimate,real));
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

    private void writeDetailRecord(String name, String path) throws IOException {
        File file = new File(path);
        file.mkdirs();
        file = new File(file,name+"-"+startDate.toString());
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(fileText.getBytes());
        fos.close();
    }

    private static void writeSummaryRecord(String record, String path) throws IOException {
        record +="\n";
        File file = new File(path);
        file.mkdirs();
        file = new File(file,"summary.tsv");
        if (file.exists()){
            FileOutputStream fos = new FileOutputStream(file,true);
            fos.write(record.getBytes());
            fos.close();
        }
        else {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(SUMMARY_HEADER.getBytes());
            fos.write(record.getBytes());
            fos.close();
        }

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

    private static String[] readMetadata(String name, File file) throws IOException {
        BufferedReader is = new BufferedReader(new FileReader(file));
        while(true){
            String line = is.readLine();
            if (line==null){
                throw new IllegalArgumentException("No metadata for dataset "+name);
            }
            String[] fields = line.split("\t");
            if (fields[0].equals(name)){
                is.close();
                return fields;
            }
        }
    }

    private static void writeMetadata(String name, String metadata, File file) throws IOException {
        BufferedReader is;
        try {
            is = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e){
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(metadata.getBytes());
            fos.close();
            return;
        }
        String newFile = "";
        boolean found = false;
        while(true){
            String line = is.readLine();
            if (line==null){
                break;
            }
            String[] fields = line.split("\t");
            if (fields[0].equals(name)){
                newFile +=metadata+"\n";
                found=true;
            } else {
                newFile+=line+"\n";
            }
        }
        if (!found){
            newFile+=metadata;
        }
        is.close();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(newFile.getBytes());
        fos.close();
    }

    private static Map<String,Object> describeMetric (String metricName, Config c, boolean isLocal){
        String type = isLocal? "local" : "universal";
        if (metricName.equals("default")){
            metricName = c.getString("sr.metric."+type +".default");
        }
        return c.getObject("sr.metric."+type +"."+ metricName).unwrapped();
    }

    private static String describeDisambiguator(String disambigName, Config c){
        if (disambigName.equals("default")){
            disambigName = c.getString("sr.disambig.default");
        }
        Map disambigConf = c.getObject("sr.disambig." + disambigName).unwrapped();
        if (disambigConf.containsKey("phraseAnalyzer")&&disambigConf.get("phraseAnalyzer").equals("default")){
            disambigConf.put("phraseAnalyzer",c.getString("phrases.analyzer.default"));
        }
        return disambigName+"="+disambigConf.toString();

    }

    public static void main(String[] args) throws DaoException, ConfigurationException, IOException {
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
                        .withDescription("the set of gold standard datasets to train on")
                        .create("g"));

        //Specify the Metrics
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("metric")
                        .withDescription("set a local metric")
                        .create("m"));
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

        //Specify the Folds
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("folds")
                        .withDescription("set the number of folds to evaluate on" )
                        .create("k"));


        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("MetricTrainer", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator c = env.getConfigurator();


        DatasetDao datasetDao = new DatasetDao();
        String datasetPath = c.getConf().get().getString("sr.dataset.path");
        List<String> datasetNames= new ArrayList<String>();
        Language lang=null;
        List<Dataset> datasets = new ArrayList<Dataset>();
        String mode;

        int k =  cmd.hasOption("k")
                ? Integer.parseInt(cmd.getOptionValue("k" ))
                : DEFAULT_SPLITS;

        if (!cmd.hasOption("u")&&!cmd.hasOption("m")){
            throw new IllegalArgumentException("Must specify a metric to evaluate.");
        }
        if (cmd.hasOption("u")&&cmd.hasOption("m")){
            throw new IllegalArgumentException("Can only operate on one metric at a time");
        }
        if (cmd.hasOption("r")){
            if (cmd.hasOption("k")||cmd.hasOption("g")||cmd.hasOption("d")||cmd.hasOption("x")){
                throw new IllegalArgumentException("Options d, g, k, and x are invalid with option r");
            } else {
                mode = "across-dataset";
                String datasetName = cmd.getOptionValue("r");
                datasetNames.add(datasetName);
                k=0;
                String[] metadata = readMetadata(datasetName,new File(c.getConf().get().getString("sr.dataset.metadata")));
                lang = Language.getByLangCode(metadata[1]);
                k = Integer.valueOf(metadata[2]);
                for (int i=1; i<k+1; i++){
                    String directory = datasetPath+datasetName+"/";
                    (new File(directory)).mkdirs();
                    datasets.add(datasetDao.read(lang, new File(directory+i+"of"+k+".txt").getAbsolutePath()));
                }

            }
        } else if (cmd.hasOption("g")){
            lang = env.getLanguages().getDefaultLanguage();
            mode = cmd.hasOption("x") ? cmd.getOptionValue("x") : "within-dataset";
            for (String dsName : cmd.getOptionValues("g")) {
                List<String> languages = c.getConf().get().getStringList("sr.dataset.sets."+dsName);
                if (languages.contains(lang.getLangCode())){
                    datasets.add(datasetDao.read(lang,new File(datasetPath, dsName).getAbsolutePath()));
                }
                datasetNames.add(dsName);
            }
        } else {
            throw new IllegalArgumentException("Must specify a dataset using either -g or -r");
        }

        List<Dataset> allTrain = new ArrayList<Dataset>();
        List<Dataset> allTest = new ArrayList<Dataset>();

        if (mode.equals("none")) {
            for (Dataset ds : datasets) {
                allTrain.add(ds);
                allTest.add(ds);
            }
        } else if (mode.equals("within-dataset")) {
            for (Dataset ds : datasets) {
                makeFolds(ds.split(k), allTrain, allTest);
            }
            if (cmd.hasOption("d")){
                String saveName = cmd.getOptionValue("d");
                for (int i=0; i<allTest.size(); i++){
                    String directory = datasetPath+saveName+"/";
                    (new File(directory)).mkdirs();
                    String name = directory+(i+1)+"of"+allTest.size()+".txt";
                    datasetDao.write(allTest.get(i), new File(name).getAbsolutePath());
                }
                String metadata = saveName+"\t"+lang.getLangCode()+"\t"+k*datasets.size()+"\t"+datasetNames.toString();
                writeMetadata(saveName, metadata, new File(c.getConf().get().getString("sr.dataset.metadata")));
            }
        } else if (mode.equals("across-dataset")) {
            makeFolds(datasets, allTrain, allTest);
        } else {
            System.err.println("Unknown mode: " + mode);
            System.exit(1);
        }

        Map<String,Double> pearsonScores = new HashMap<String, Double>();
        Map<String,Double> spearmanScores = new HashMap<String, Double>();
        Map<String,Integer> missing = new HashMap<String, Integer>();
        Map<String,Integer> failed = new HashMap<String, Integer>();
        String metricName = cmd.hasOption("m")? cmd.getOptionValue("m"): cmd.getOptionValue("u");
        String recordPath = c.getConf().get().getString("sr.dataset.records");


        CrossValidation crossValidation = new CrossValidation();
        //Run evaluation
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
                crossValidation.evaluate(sr, test);
            } else if (usr!=null){
                usr.trainSimilarity(train);
                crossValidation.evaluate(usr, test);
            }

            //Recording the results
            if ((!mode.equals("within-dataset")&&!cmd.hasOption("r"))||(i+1)%k==0){
                String setName;
                double pearson=0;
                double spearman=0;
                if (mode.equals("within-dataset")||cmd.hasOption("r")){
                    setName = datasetNames.get(i/k);
                    for (double score : crossValidation.pearsonScores){
                        pearson+=score;
                    }
                    pearson = pearson/crossValidation.pearsonScores.size();
                    for (double score : crossValidation.spearmanScores){
                        spearman+=score;
                    }
                    spearman = spearman/crossValidation.spearmanScores.size();
                } else {
                    setName = datasetNames.get(i);
                    pearson = crossValidation.pearsonScores.get(0);
                    spearman = crossValidation.spearmanScores.get(0);
                }
                //build summary
                crossValidation.writeDetailRecord(metricName+"-"+setName, recordPath);
                pearsonScores.put(setName, pearson);
                spearmanScores.put(setName,spearman);
                missing.put(setName,crossValidation.missing);
                failed.put(setName,crossValidation.failed);
                Map metricMap = describeMetric(metricName, c.getConf().get(), cmd.hasOption("m"));
                String disambigDescription;
                if (metricMap.containsKey("disambiguator")){
                    disambigDescription = describeDisambiguator((String)metricMap.get("disambiguator"),c.getConf().get());
                } else {
                    disambigDescription = "none";
                }
                String metricDescription = metricName+"="+metricMap.toString();

                String summary = lang.getEnLangName() + "\t";
                summary += metricName + "\t";
                summary += disambigDescription+"\t";
                summary += setName + "\t";
                summary += mode + "\t";
                summary += pearson + "\t";
                summary += spearman + "\t";
                summary += crossValidation.missing + "\t";
                summary += crossValidation.failed + "\t";
                summary += metricDescription + "\t";
                summary += metricName+"-"+setName+"-"+crossValidation.startDate.toString();
                //write summary and clear
                writeSummaryRecord(summary,recordPath);
                crossValidation = new CrossValidation();
            }
        }



        //Reporting results to the user
        double pScoreSum = 0.0;
        double sScoreSum = 0.0;
        int totalMissing = 0;
        int totalFailed = 0;
        int numSets = pearsonScores.size();
        assert (numSets==spearmanScores.size());
        assert (numSets==missing.size());
        assert (numSets==failed.size());
        for (String setName : pearsonScores.keySet()){
            System.out.println(setName+":");
            System.out.println("\tPearson score: "+pearsonScores.get(setName));
            pScoreSum+=pearsonScores.get(setName);
            System.out.println("\tSpearman score: "+spearmanScores.get(setName));
            sScoreSum+=spearmanScores.get(setName);
            System.out.println("\tMissing: "+missing.get(setName));
            totalMissing+=missing.get(setName);
            System.out.println("\tFailed: "+failed.get(setName));
            totalFailed+=failed.get(setName);
        }

        System.out.println("Overall pearson: "+pScoreSum/numSets);
        System.out.println("Overall spearman: "+sScoreSum/numSets);
        System.out.println("Total missing: "+totalMissing);
        System.out.println("Total failed: "+totalFailed);
    }

}