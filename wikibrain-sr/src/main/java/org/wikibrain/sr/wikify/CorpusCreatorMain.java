package org.wikibrain.sr.wikify;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.phrases.LinkProbabilityDao;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Shilad Sen
 */
public class CorpusCreatorMain {
    private static final Logger LOG = LoggerFactory.getLogger(CorpusCreatorMain.class);


    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("output")
                        .withDescription("corpus output directory (existing data will be overwritten)")
                        .create("o"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("corpus")
                        .withDescription("Name of corpus configuration")
                        .create("p"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("fractionWikified")
                        .withDescription("Desired fraction of terms that should be wikified (only for websail).")
                        .create("f"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("desiredRecall")
                        .withDescription("Desired recall of wikification (only for websail).")
                        .create("r"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("desiredPrecision")
                        .withDescription("Desired precision of wikification (only for websail).")
                        .create("p"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("CorpusCreatorMain", options);
            return;
        }
        Env env = new EnvBuilder(cmd).build();
        Language lang = env.getDefaultLanguage();

        LinkProbabilityDao lpd = env.getComponent(LinkProbabilityDao.class, lang);
        lpd.useCache(true);
        lpd.buildIfNecessary();

        String corpusName = cmd.getOptionValue("corpus", "wikified");
        Corpus corpus = env.getComponent(Corpus.class, corpusName, lang);
        if (cmd.hasOption("fractionWikified")) {
            if (corpus.getWikifer() instanceof WebSailWikifier) {
                double frac = Double.valueOf(cmd.getOptionValue("fractionWikified"));
                ((WebSailWikifier)corpus.getWikifer()).setDesiredWikifiedFraction(frac);
            } else {
                System.err.println("fractionWikified only valid for WebSail wikified corpora.");
                System.exit(1);
            }
        }
        if (cmd.hasOption("desiredRecall")) {
            if (corpus.getWikifer() instanceof WebSailWikifier) {
                double r = Double.valueOf(cmd.getOptionValue("desiredRecall"));
                ((WebSailWikifier)corpus.getWikifer()).setDesiredRecall(r);
            } else {
                System.err.println("fractionWikified only valid for WebSail wikified corpora.");
                System.exit(1);
            }
        }
        if (cmd.hasOption("desiredPrecision")) {
            if (corpus.getWikifer() instanceof WebSailWikifier) {
                double p = Double.valueOf(cmd.getOptionValue("desiredPrecision"));
                ((WebSailWikifier)corpus.getWikifer()).setDesiredPrecision(p);
            } else {
                System.err.println("fractionWikified only valid for WebSail wikified corpora.");
                System.exit(1);
            }
        }
        File output = new File(cmd.getOptionValue("output"));
        if (!output.isDirectory()) {
            output.mkdirs();
        }
        corpus.setDirectory(output);
        corpus.create();
    }
}
