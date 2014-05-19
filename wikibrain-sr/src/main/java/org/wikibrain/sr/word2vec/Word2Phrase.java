package org.wikibrain.sr.word2vec;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.lang.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.nlp.NGramCreator;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.phrases.AnchorTextPhraseAnalyzer;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.phrases.PhraseAnalyzerDao;
import org.wikibrain.utils.Scoreboard;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.utils.WpStringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Given a corpus of words and phrases, iteratively concatenates bigrams that are
 * likely to be more meaningful together than apart.
 *
 * ie "speed of light" => "speed_of light" (pass 1) -> "speed_of_light" (pass 2)
 *
 * @author Shilad Sen
 */
public class Word2Phrase {
    private static final Logger LOG = Logger.getLogger(Word2Phrase.class.getName());
    private final PhraseAnalyzerDao phraseDao;
    private final Language language;

    private int minReduce = 2;
    private int minCount = 5;
    private int threshold = -1;

    private NGramCreator nGramCreator = new NGramCreator();
    private StringTokenizer tokenizer = new StringTokenizer();

    /**
     * Counts of word and phrase hashes.
     */
    private TLongIntMap counts = new TLongIntHashMap();

    /**
     * Total size of corpus (i.e. sum of word freqs).
     */
    private long totalWords = 0;

    public Word2Phrase(Language language, PhraseAnalyzerDao phraseDao) {
        this.language = language;
        this.phraseDao = phraseDao;
    }

    public void concatenateBigrams(File input, File output, int maxWords) throws IOException {
        File[][] ioPairs = new File[maxWords-1][2];
        for (int i = 0; i < ioPairs.length; i++) {
            ioPairs[i][0] = new File(input.getAbsolutePath() + "." + (i+1));
            ioPairs[i][1] = new File(input.getAbsolutePath() + "." + (i+2));
        }
        ioPairs[0][0] = input;
        ioPairs[ioPairs.length - 1][1] = output;

        for (int i = 0; i < ioPairs.length; i++) {
            LOG.info("pass " + i + ": joining phrases of length " + (i+1) + " to " + (i+2));
            File in = ioPairs[i][0];
            File out = ioPairs[i][1];
            countCorpus(in);
            if (i == 0) {
                this.threshold = learnThreshold(i+2);        // index 0 goes from unigrams to bigrams
            } else {
                this.threshold = Math.max(5, this.threshold/3);
            }
            processFile(in, out, i + 2);
        }
        writeDictionary(output, new File(output.getParentFile(), "phrases.txt"));
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
        int pa = getWordCount(token1);
        int pb = getWordCount(token2);
        if (pa < minCount || pb < minCount) {
            return 0.0;
        }
        int pab = getWordCount(token1 + " " + token2);
        return  1.0 * (pab - minCount) * totalWords / (pa * pb);
    }

    private int getWordCount(String word) {
        return counts.get(WpStringUtils.longHashCode(word));
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
                if (getWordCount(first) >= minCount && getWordCount(second) >= minCount) {
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
            topFirst.add(bigram[0], getWordCount(bigram[0]));
            topSecond.add(bigram[1], getWordCount(bigram[1]));
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

    private void countCorpus(File input) throws IOException {
        totalWords = 0;
        counts.clear();
        LOG.info("counting unigrams and bigrams in " + input);
        BufferedReader reader = WpIOUtils.openBufferedReader(input);
        while (true) {
            String sentence = reader.readLine();
            if (sentence == null) {
                break;
            }
            List<String> tokens = new ArrayList<String>();
            for (String token : sentence.split(" +")) {
                Matcher m = Word2VecUtils.PATTERN_ID.matcher(token);
                if (m.matches()) {
                    token = m.group(1);
                }
                tokens.add(token);
                totalWords++;
            }

            for (String ngram : nGramCreator.getNGrams(tokens, 1, 2)) {
                long hash = WpStringUtils.longHashCode(ngram);
                counts.adjustOrPutValue(hash, 1, 1);
            }
        }
        LOG.info("discovered " + counts.size() + " unigrams and bigrams in " + input + " with total count " + totalWords);
    }


    private void writeDictionary(File corpus, File dictionaryFile) throws IOException {
        LOG.info("calculating counts for dictionary");
        TLongIntMap counts = new TLongIntHashMap();
        File tmpFile = File.createTempFile("dictionary", ".txt");
        tmpFile.deleteOnExit();
        tmpFile.delete();
        BufferedWriter writer = WpIOUtils.openWriter(tmpFile);
        BufferedReader reader = WpIOUtils.openBufferedReader(corpus);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String[] tokens = line.trim().split(" +");
            for (int i = 0; i < tokens.length; i++) {
                long h = Word2VecUtils.hashWord(tokens[i]);
                if (!counts.containsKey(h)) {
                    writer.write(tokens[i] + "\n");
                }
                counts.adjustOrPutValue(h, 1, 1);
            }
        }
        writer.close();
        reader.close();


        LOG.info("writing " + counts.size() + " dictionary...");
        reader = WpIOUtils.openBufferedReader(tmpFile);
        writer = WpIOUtils.openWriter(dictionaryFile);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String phrase = line.trim();
            long hash = Word2VecUtils.hashWord(phrase);
            writer.write("" + counts.get(hash) + " " + phrase + "\n");
        }
        writer.close();
        reader.close();
    }

    public static void main(String args[]) throws IOException, ConfigurationException {
        Env env = EnvBuilder.envFromArgs(args);
        File input = new File("word2vec/simple.all/corpus.txt");
        File output = new File("word2vec/simple.all/corpus.phrases.txt");
        AnchorTextPhraseAnalyzer phraseAnalyzer =
                (AnchorTextPhraseAnalyzer) env.getConfigurator().get(PhraseAnalyzer.class, "anchortext");
        PhraseAnalyzerDao dao = phraseAnalyzer.getDao();

        Word2Phrase w2p = new Word2Phrase(env.getLanguages().getDefaultLanguage(), dao);
        w2p.concatenateBigrams(input, output, 4);
    }
}
