package org.wikibrain.sr.evaluation;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.Language;

import java.io.File;
import java.io.IOException;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class QualitativeAnalyzer {
    private final Env env;
    private final Language language;

    public QualitativeAnalyzer(Env env) {
        this.env = env;
        this.language = env.getLanguages().getDefaultLanguage();
    }

    public void analyze(String datasetName, int runNumber) throws IOException, java.text.ParseException {
        File parentDir = FileUtils.getFile(
                env.getConfiguration().get().getString("sr.dataset.records"),
                "local-similarity",
                language.getLangCode(),
                datasetName
        );
        if (!parentDir.isDirectory()) {
            throw new IllegalArgumentException("directory " + parentDir + " does not exist");
        }
        List<File> toAnalyze = new ArrayList<File>();
        for (File file : parentDir.listFiles()) {
            if (file.getName().startsWith(runNumber + "-")) {
                toAnalyze.add(file);
            }
        }
        if (toAnalyze.isEmpty()) {
            throw new IllegalArgumentException("No matching files found in directory " + parentDir);
        }
        for (File file : toAnalyze) {
            analyze(file);
        }
    }

    public void analyze(File dir) throws IOException, java.text.ParseException {
        File logFile = new File(dir, "overall.log");
        SimilarityEvaluationLog log = SimilarityEvaluationLog.read(logFile);
        List<KnownSimGuess> guesses = log.getGuesses();
        showClosest(guesses);
        showInfluential(guesses);
    }

    private void showClosest(List<KnownSimGuess> guesses) {
        Collections.sort(guesses, new Comparator<KnownSimGuess>() {
            @Override
            public int compare(KnownSimGuess g1, KnownSimGuess g2) {
                Double e1 = g1.getError2();
                Double e2 = g2.getError2();
                int r = e1.compareTo(e2);
                if (r == 0) {
                    r = g1.getPhrase1().compareTo((g2.getPhrase1()));
                }
                if (r == 0) {
                    r = g1.getPhrase2().compareTo((g2.getPhrase2()));
                }
                return r;
            }
        });
        System.out.println("\nclosest guesses:");
        for (int i = 0; i < 50 && i < guesses.size()/2; i++) {
            KnownSimGuess g = guesses.get(i);
            System.out.println(String.format("%d. err=%+.3f '%s' vs. '%s'; actual=%.3f pred=%.3f",
                    (i+1), g.getError(), g.getPhrase1(), g.getPhrase2(), g.getActual(), g.getGuess()));
        }

        System.out.println("\nfurthest guesses:");
        for (int i = 0; i < 50 && i < guesses.size()/2; i++) {
            KnownSimGuess g = guesses.get(guesses.size() - i - 1);
            System.out.println(String.format("%d. err=%+.3f '%s' vs. '%s'; actual=%.3f pred=%.3f",
                    (i+1), g.getError(), g.getPhrase1(), g.getPhrase2(), g.getActual(), g.getGuess()));
        }
    }

    private double getGuessMean(List<KnownSimGuess> guesses) {
        double guessMean = 0.0;
        int count = 0;
        for (KnownSimGuess g : guesses) {
            if (g.hasGuess()) {
                count++;
                guessMean += g.getGuess();
            }
        }
        if (count == 0.0) {
            return 0.0;
        } else {
            return guessMean / count;
        }
    }

    private double getActualMean(List<KnownSimGuess> guesses) {
        double actualMean = 0.0;
        int count = 0;
        for (KnownSimGuess g : guesses) {
            if (g.hasGuess()) {
                count++;
                actualMean += g.getActual();
            }
        }
        if (count == 0.0) {
            return 0.0;
        } else {
            return actualMean / count;
        }
    }

    private void showInfluential(List<KnownSimGuess> guesses) {
        final double guessMean = getGuessMean(guesses);
        final double actualMean = getActualMean(guesses);

        Collections.sort(guesses, new Comparator<KnownSimGuess>() {
            @Override
            public int compare(KnownSimGuess g1, KnownSimGuess g2) {
                Double s1 = (g1.getGuess() - guessMean) * (g1.getActual() - actualMean);
                Double s2 = (g2.getGuess() - guessMean) * (g2.getActual() - actualMean);
                int r = s1.compareTo(s2);
                if (r == 0) {
                    r = g1.getPhrase1().compareTo((g2.getPhrase1()));
                }
                if (r == 0) {
                    r = g1.getPhrase2().compareTo((g2.getPhrase2()));
                }
                return r;
            }
        });
        System.out.println("\nmost influential good guesses:");
        for (int i = 0; i < 50 && i < guesses.size()/2; i++) {
            KnownSimGuess g = guesses.get(i);
            double s = (g.getGuess() - guessMean) * (g.getActual() - actualMean);
            System.out.println(String.format("%d. influence=%.3f '%s' vs. '%s'; actual=%.3f pred=%.3f",
                    (i+1), s, g.getPhrase1(), g.getPhrase2(), g.getActual(), g.getGuess()));
        }

        System.out.println("\nmost influential bad guesses:");
        for (int i = 0; i < 50 && i < guesses.size()/2; i++) {
            KnownSimGuess g = guesses.get(guesses.size() - i - 1);
            double s = (g.getGuess() - guessMean) * (g.getActual() - actualMean);
            System.out.println(String.format("%d. influence=%.3f '%s' vs. '%s'; actual=%.3f pred=%.3f",
                    (i+1), s, g.getPhrase1(), g.getPhrase2(), g.getActual(), g.getGuess()));
        }
    }


    public static void main(String args[]) throws ConfigurationException, IOException, java.text.ParseException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("directories")
                        .withDescription("list of directories to compare")
                        .create("d"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("gold")
                        .hasArg()
                        .withValueSeparator(',')
                        .withDescription("gold standard name (for use with -n)")
                        .create("g"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("number")
                        .hasArg()
                        .withValueSeparator(',')
                        .withDescription("list of run numbers to compare")
                        .create("b"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            System.exit(1);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        QualitativeAnalyzer analyzer = new QualitativeAnalyzer(env);
        if (cmd.hasOption("d")) {
            for (String dir : cmd.getOptionValues("d")) {
                analyzer.analyze(new File(dir));
            }
        } else if (cmd.hasOption("b") && cmd.hasOption("g")) {
            analyzer.analyze(cmd.getOptionValue("g"), Integer.valueOf(cmd.getOptionValue("n")));
        } else {
            System.err.println("One of -d or (-b and -g) must be specified");
            new HelpFormatter().printHelp("DumpLoader", options);
            System.exit(1);
            return;
        }
    }
}
