package org.wikapidia.sr;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;

/**
 * @author Matt Lesicko
 */
public class MatrixBuilder {
    public static void  main(String args[]) throws ConfigurationException, IOException, WikapidiaException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                    .hasArg()
                    .withLongOpt("universal")
                    .withDescription("set a universal metric")
                    .create("u"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("metric")
                        .withDescription("set a local metric")
                        .create("m"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("max-results")
                        .withDescription("maximum number of results")
                        .create("r"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("MatrixBuilder", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator c = env.getConfigurator();

        if (!cmd.hasOption("m")&&!cmd.hasOption("u")){
            System.err.println("Must specify a metric using -u or -m");
            new HelpFormatter().printHelp("MatrixBuilder", options);
            return;
        }

        LanguageSet languages = env.getLanguages();
        String path = c.getConf().get().getString("sr.metric.path");
        int maxResults = cmd.hasOption("r")? Integer.parseInt(cmd.getOptionValue("r")) : c.getConf().get().getInt("sr.normalizer.defaultmaxresults");

        LocalSRMetric sr=null;
        UniversalSRMetric usr=null;
        if (cmd.hasOption("m")){
            FileUtils.deleteDirectory(new File(path + cmd.getOptionValue("m") + "/" + "matrix/"));
            (new File(path + cmd.getOptionValue("m") + "/" + "matrix/")).mkdirs();
            sr = c.get(LocalSRMetric.class,cmd.getOptionValue("m"));
            sr.writeCosimilarity(path,languages,maxResults);
        }
        if (cmd.hasOption("u")){
            FileUtils.deleteDirectory(new File(path+cmd.getOptionValue("u")+"/"+"matrix/"));
            (new File(path + cmd.getOptionValue("u") + "/" + "matrix/")).mkdirs();
            usr = c.get(UniversalSRMetric.class,cmd.getOptionValue("u"));
            usr.writeCosimilarity(path,maxResults);
        }
    }
}
