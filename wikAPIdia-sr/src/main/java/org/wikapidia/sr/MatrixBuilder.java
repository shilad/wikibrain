package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.utils.WpIOUtils;

import java.io.BufferedReader;
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
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("rowids")
                        .withDescription("page ids for rows")
                        .create("p"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("colids")
                        .withDescription("page ids for columns")
                        .create("q"));
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

        Env env = new EnvBuilder(cmd).build();
        Configurator c = env.getConfigurator();

        if (!cmd.hasOption("m")&&!cmd.hasOption("u")){
            System.err.println("Must specify a metric using -u or -m");
            new HelpFormatter().printHelp("MatrixBuilder", options);
            return;
        }

        TIntSet rowIds = null;
        TIntSet colIds = null;

        if (cmd.hasOption("p")) {
            rowIds = readIds(cmd.getOptionValue("p"));
        }
        if (cmd.hasOption("q")) {
            colIds = readIds(cmd.getOptionValue("q"));
        }

        LanguageSet languages = env.getLanguages();
        String path = c.getConf().get().getString("sr.metric.path");
        int maxResults = cmd.hasOption("r")? Integer.parseInt(cmd.getOptionValue("r")) : c.getConf().get().getInt("sr.normalizer.defaultmaxresults");

        MonolingualSRMetric sr=null;
        UniversalSRMetric usr=null;
        if (cmd.hasOption("m")){
            Language language = languages.getDefaultLanguage();
            sr = c.get(MonolingualSRMetric.class,cmd.getOptionValue("m"), "language", language.getLangCode());
            sr.writeCosimilarity(path, maxResults, rowIds, colIds);
        }
        if (cmd.hasOption("u")){
            usr = c.get(UniversalSRMetric.class,cmd.getOptionValue("u"));
            usr.writeCosimilarity(path,maxResults);
        }
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
}
