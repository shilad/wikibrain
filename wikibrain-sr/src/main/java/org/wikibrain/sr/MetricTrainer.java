package org.wikibrain.sr;

import org.apache.commons.cli.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Matt Lesciko
 * @author Ben Hillmann
 */
public class MetricTrainer {

    public static void main(String[] args) throws ConfigurationException, DaoException, IOException, WikiBrainException {
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
        LanguageSet allLangs = env.getLanguages();

        DatasetDao datasetDao = env.getConfigurator().get(DatasetDao.class);

        List<String> datasetNames;
        if (cmd.hasOption("g")){
            datasetNames = Arrays.asList(cmd.getOptionValues("g"));
        } else {
            datasetNames = c.getConf().get().getStringList("sr.dataset.defaultsets");
        }

        List<Dataset> datasets = new ArrayList<Dataset>();
        for (String name : datasetNames) {
            DatasetDao.Info info = datasetDao.getInfo(name);
            Collection<Language> possibleLang = CollectionUtils.intersection(
                                                        info.getLanguages().getLanguages(),
                                                        allLangs.getLanguages());
            if (possibleLang.isEmpty()) {
                System.err.println("dataset " + name + " is a language other than " + allLangs);
                System.exit(1);
            }
            if (possibleLang.size() > 1) {
                System.err.println("dataset " + name + " supports more than one language of " + allLangs + " please specify");
                System.exit(1);
            }
            Language lang = possibleLang.iterator().next();
            if (datasets.size() > 0 && !lang.equals(datasets.get(0).getLanguage())) {
                System.err.println("Language mismatch in datasets " + name + " and " + datasets.get(0).getName());
                System.exit(1);
            }
            datasets.add(datasetDao.get(lang, name));
        }

        SRMetric sr=null;
        if (cmd.hasOption("m")){
            Language language = datasets.get(0).getLanguage();
            FileUtils.deleteDirectory(new File(path+cmd.getOptionValue("m")+"/"+"normalizer/"));
            sr = c.get(SRMetric.class,cmd.getOptionValue("m"), "language", language.getLangCode());
        }

        Dataset dataset = new Dataset(datasets);
        if (sr!=null){
            sr.trainMostSimilar(dataset, maxResults, null);
            sr.trainSimilarity(dataset);
            sr.write();
            sr.read();
        }
    }
}
