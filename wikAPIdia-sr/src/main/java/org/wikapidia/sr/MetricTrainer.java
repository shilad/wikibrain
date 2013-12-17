package org.wikapidia.sr;

import org.apache.commons.cli.*;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.dataset.DatasetDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Matt Lesciko
 * @author Ben Hillmann
 */
public class MetricTrainer {

    public static void main(String[] args) throws ConfigurationException, DaoException, IOException, WikapidiaException {
        Options options = new Options();

        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("universal")
                        .withDescription("set a universal metric")
                        .create("u"));
        //Number of Max Results(otherwise take from config)
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("max-results")
                        .withDescription("maximum number of results")
                        .create("r"));
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

        Env env = new EnvBuilder(cmd)
                        .setProperty("sr.metric.training", true)
                        .build();
        Configurator c = env.getConfigurator();

        if (!cmd.hasOption("m")&&!cmd.hasOption("u")){
            System.err.println("Must specify a metric to train using -m or -u.");
            new HelpFormatter().printHelp("MetricTrainer", options);
            return;
        }



        int maxResults = cmd.hasOption("r")? Integer.parseInt(cmd.getOptionValue("r")) : c.getConf().get().getInt("sr.normalizer.defaultmaxresults");



        String path = c.getConf().get().getString("sr.metric.path");
        LanguageSet validLanguages = env.getLanguages();

        List<Dataset> datasets = new ArrayList<Dataset>();
        DatasetDao datasetDao = new DatasetDao();

        List<String> datasetNames;
        if (cmd.hasOption("g")){
            datasetNames = Arrays.asList(cmd.getOptionValues("g"));
        } else {
            datasetNames = c.getConf().get().getStringList("sr.dataset.defaultsets");
        }
        for (String name : datasetNames){
            List<String> languages = c.getConf().get().getStringList("sr.dataset.sets."+name);
            for (String langCode : languages){
                Language language = Language.getByLangCode(langCode);
                if (validLanguages==null||validLanguages.containsLanguage(language)){
                    datasets.add(datasetDao.get(language, name));
                }
            }
        }


        List<Language> languages = new ArrayList<Language>();
        for (Dataset dataset : datasets){
            languages.add(dataset.getLanguage());
        }
        LanguageSet languageSet = new LanguageSet(languages);

        LocalSRMetric sr=null;
        UniversalSRMetric usr=null;
        if (cmd.hasOption("m")){
            FileUtils.deleteDirectory(new File(path+cmd.getOptionValue("m")+"/"+"normalizer/"));
            sr = c.get(LocalSRMetric.class,cmd.getOptionValue("m"));
        }
        if (cmd.hasOption("u")){
            FileUtils.deleteDirectory(new File(path+cmd.getOptionValue("u")+"/"+"normalizer/"));
            usr = c.get(UniversalSRMetric.class,cmd.getOptionValue("u"));
        }

        double mostSimilarThreshold = c.getConf().get().getDouble("sr.dataset.mostSimilarThreshold");

        for (Dataset dataset: datasets) {
            if (usr!=null){
                usr.trainSimilarity(dataset);
                usr.trainMostSimilar(dataset.prune(mostSimilarThreshold, 1.1),maxResults,null);
            }
            if (sr!=null){
                sr.trainDefaultSimilarity(dataset);
                sr.trainDefaultMostSimilar(dataset.prune(mostSimilarThreshold, 1.1),maxResults,null);
                sr.trainSimilarity(dataset);
                sr.trainMostSimilar(dataset.prune(mostSimilarThreshold, 1.1),maxResults,null);
            }
        }

        if (usr!=null){
            (new File(path + cmd.getOptionValue("u") + "/" + "normalizer/")).mkdirs();
            usr.write(path);
            usr.read(path);
        }
        if (sr!=null){
            (new File(path + cmd.getOptionValue("m") + "/" + "normalizer/")).mkdirs();
            sr.write(path);
            sr.read(path);
        }



    }
}
