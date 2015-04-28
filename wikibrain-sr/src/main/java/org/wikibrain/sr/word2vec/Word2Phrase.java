package org.wikibrain.sr.word2vec;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.nlp.Dictionary;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.phrases.AnchorTextPhraseAnalyzer;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.phrases.PhraseAnalyzerDao;
import org.wikibrain.utils.Scoreboard;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a corpus of words and phrases, iteratively concatenates bigrams that are
 * likely to be more meaningful together than apart.
 *
 * ie "speed of light" =&gt; "speed_of light" (pass 1) -&gt; "speed_of_light" (pass 2)
 *
 * @author Shilad Sen
 */
public class Word2Phrase {
    private static final Logger LOG = LoggerFactory.getLogger(Word2Phrase.class);
    private final PhraseAnalyzerDao phraseDao;
    private final Language language;

    private int minReduce = 2;
    private int minCount = 5;
    private int threshold = -1;

    private StringTokenizer tokenizer = new StringTokenizer();

    /**
     * Counts of word and phrases.
     */
    private Dictionary dictionary;

    public Word2Phrase(Language language, PhraseAnalyzerDao phraseDao) {
        this.language = language;
        this.phraseDao = phraseDao;
    }

    public void concatenateBigrams(File inputDir, File outputDir, int maxWords) throws IOException {
        File[][] ioPairs = new File[maxWords-1][2];
        for (int i = 0; i < ioPairs.length; i++) {
            ioPairs[i][0] = new File(outputDir, "phrases.txt." + (i+1));
            ioPairs[i][1] = new File(outputDir, "phrases.txt." + (i+2));
        }
        ioPairs[0][0] = new File(inputDir, "corpus.txt");
        ioPairs[ioPairs.length - 1][1] = new File(outputDir, "corpus.txt");

        for (int i = 0; i < ioPairs.length; i++) {
            LOG.info("pass " + i + ": joining phrases of length " + (i+1) + " to " + (i+2));
            File in = ioPairs[i][0];
            File out = ioPairs[i][1];
            dictionary = new Dictionary(language, Dictionary.WordStorage.ON_DISK);
            dictionary.setCountBigrams(true);
            dictionary.countNormalizedFile(in);
            if (i == 0 && this.threshold < 0) {
                this.threshold = learnThreshold(i+2);        // index 0 goes from unigrams to bigrams
            } else {
                this.threshold = Math.max(5, this.threshold/3);
            }
            processFile(in, out, i + 2);
        }
        dictionary.write(new File(outputDir, "dictionary.txt"));
    }

    private void processFile(File input, File output, int maxWords) throws IOException {
        BufferedReader reader = WpIOUtils.openBufferedReader(input);
        BufferedWriter writer = WpIOUtils.openWriter(output);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.trim().split(" +");
            StringBuilder newLine = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                if (i > 0) {
                    if (shouldConcatenate(tokens[i-1], tokens[i], maxWords)) {
//                        System.out.println("concatenating " + tokens[i-1] + " and " + tokens[i]);
                        newLine.append('_');
                    } else {
                        newLine.append(' ');
                    }
                }
                newLine.append(tokens[i]);
            }
            newLine.append('\n');
            writer.write(newLine.toString());
        }
        reader.close();
    }

    private boolean shouldConcatenate(String token1, String token2, int maxWords) {
        int numWordsInToken1 = StringUtils.countMatches(token1, "_") + 1;
        int numWordsInToken2 = StringUtils.countMatches(token1, "_") + 1;
        return (
                numWordsInToken1 == maxWords - 1 &&
                numWordsInToken2 == 1 &&
                scoreBigram(token1, token2) >= threshold
        );
    }

    private double scoreBigram(String token1, String token2) {
        if (Word2VecUtils.PATTERN_ID.matcher(token1).matches()) {
            return 0.0;
        }
        int pa = dictionary.getUnigramCount(token1);
        int pb = dictionary.getUnigramCount(token2);
        if (pa < minCount || pb < minCount) {
            return 0.0;
        }
        int pab = dictionary.getBigramCount(token1, token2);
        return  1.0 * (pab - minCount) * dictionary.getTotalCount() / (pa * pb);
    }

    public int learnThreshold(int numWords) {
        List<String[]> bigrams = getKnownBigrams(numWords);
        if (bigrams.isEmpty()) {
            throw new IllegalStateException("Found no anchor texts of length " + numWords);
        }
        List<String[]> nonBigrams = getNonBigramSample(bigrams, bigrams.size());

        List<Double> bigramScores = new ArrayList<Double>();
        List<Double> nonBigramScores = new ArrayList<Double>();

        for (String [] bigram : bigrams) {
            bigramScores.add(scoreBigram(bigram[0], bigram[1]));
        }
        for (String [] bigram : nonBigrams) {
            nonBigramScores.add(scoreBigram(bigram[0], bigram[1]));
        }


        Collections.sort(bigramScores);
        Collections.sort(nonBigramScores);

        double bestScore = 0.0;
        int bestThreshold = 0;
        for (int t = 0; t < 1000; t++) {
            // Calculate nearest indexes for threshold in each list
            int i1 = Collections.binarySearch(bigramScores, (double)t);
            int i2 = Collections.binarySearch(nonBigramScores, (double)t);

            // Calculate number of bigrams / non-bigrams returned for each list.
            int n1 = bigramScores.size() - Math.abs(i1);
            int n2 = nonBigramScores.size() - Math.abs(i2);

            double precision = 0.0;
            double recall = 0.0;
            if (n1 + n2 > 0) {
                precision = 1.0 * n1 / (n1 + n2);
                recall = 1.0 * n1 / bigramScores.size();
                double score = (precision * recall) / (0.25 * precision + recall);
                if (score > bestScore) {
                    bestThreshold = t;
                    bestScore = score;
                }
            }
//            System.out.println("for " + n1 + ", " + n2 + " threshold " + t + ", precision is " + precision + "; recall is " + recall);

        }
        LOG.info("learned threshold " + bestThreshold + " for words of length " + numWords + " with " + bigrams.size() + " known bigrams");
        return bestThreshold;
    }

    private List<String[]> getKnownBigrams(int numWords) {
        List<String[]> bigrams = new ArrayList<String[]>();
        Iterator<String> iter = phraseDao.getAllPhrases(language);
        while (iter.hasNext()) {
            String phrase = iter.next();
            List<String> tokens = tokenizer.getWords(language, phrase);
            if (tokens.size() == numWords) {
                String first = StringUtils.join(tokens.subList(0, tokens.size() - 1), '_');
                String second = tokens.get(tokens.size() - 1);
                int n1 = dictionary.getUnigramCount(first);
                int n2 = dictionary.getUnigramCount(second);
                if (n1 >= minCount && n2 >= minCount) {
                    bigrams.add(new String[]{ first, second });
                }
            }
        }
        return bigrams;
    }

    private List<String[]> getNonBigramSample(List<String[]> bigrams, int n) {
        Scoreboard<String> topFirst = new Scoreboard<String>(1000);
        Scoreboard<String> topSecond = new Scoreboard<String>(1000);

        Set<String> known = new HashSet<String>();
        for (String[] bigram : bigrams) {
            topFirst.add(bigram[0], dictionary.getUnigramCount(bigram[0]));
            topSecond.add(bigram[1], dictionary.getUnigramCount(bigram[1]));
            known.add(bigram[0] + "_" + bigram[1]);
        }
        Random random = new Random();
        ArrayList<String[]> nonBigrams = new ArrayList<String[]>();
        while (nonBigrams.size() < n) {
            String first = topFirst.getElement(random.nextInt(topFirst.size()));
            String second = topSecond.getElement(random.nextInt(topSecond.size()));
            if (!known.contains(first + "_" + second)) {
                nonBigrams.add(new String[] { first, second });
            }
        }
        return nonBigrams;
    }

    public static void main(String args[]) throws IOException, ConfigurationException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("input")
                        .withDescription("corpus input directory")
                        .create("i"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("output")
                        .withDescription("corpus output directory (existing data will be lost)")
                        .create("o"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("minCount")
                        .withDescription("minimum frequency for unigrams that should be collapsed")
                        .create("m"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("maxngram")
                        .withDescription("maximum number of words that should be concatenated together")
                        .create("g"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("Word2Phrase", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        AnchorTextPhraseAnalyzer phraseAnalyzer =
                (AnchorTextPhraseAnalyzer) env.getConfigurator().get(PhraseAnalyzer.class, "anchortext");
        PhraseAnalyzerDao dao = phraseAnalyzer.getDao();

        Word2Phrase w2p = new Word2Phrase(env.getLanguages().getDefaultLanguage(), dao);
        w2p.concatenateBigrams(
                new File(cmd.getOptionValue("i")),
                new File(cmd.getOptionValue("o")),
                Integer.valueOf(cmd.getOptionValue("g", "4")));
    }
}
