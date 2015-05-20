package org.wikibrain.sr.dataset;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.nlp.Dictionary;
import org.wikibrain.sr.utils.KnownSim;
import org.wikibrain.sr.wikify.Corpus;
import org.wikibrain.sr.wikify.WbCorpusLineReader;
import org.wikibrain.utils.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * Creates a fake gold standard. This is useful for training
 * language editions that do not have existing gold standards.
 * </p>
 *
 * <p>
 * A gold standard of size n is created by selecting n target words,
 * and computing the pointwise mutual information of them with k other
 * candidates. For each of the n targets w, a candidate c is selected
 * randomly, with an exponential weight favoring candidates with
 * higher PMI. The value assigned to the pair is the percentile of the
 * PMI for that pair.
 * </p>
 *
 * <p>The n target words are selected from the maxTargetRank most
 * frequent words after removing stopWordRank stop words. The candidates
 * are the maxCandidate most frequent words after removing stop words.
 * </p>
 * @author Shilad Sen
 */
public class FakeDatasetCreator {


    private int stopWordRank = 1000;
    private int maxTargetRank = 1000;
    private int maxCandidateRank = 30000;

    private final Dictionary dictionary;
    private final File path;
    private final Language lang;

    public FakeDatasetCreator(Language lang, File path) throws IOException {
        this.lang = lang;
        this.path = path;

        // Build up map from most common words (ignoring stopwords) to a numeric index
        this.dictionary = new Dictionary(lang, Dictionary.WordStorage.IN_MEMORY);
        this.dictionary.countNormalizedFile(path);
    }

    public FakeDatasetCreator(Corpus corpus) throws IOException {
        this.dictionary = new Dictionary(corpus.getLanguage(), Dictionary.WordStorage.IN_MEMORY);
        this.dictionary.read(corpus.getDictionaryFile());
        this.path = corpus.getCorpusFile();
        this.lang = corpus.getLanguage();
    }

    public void setStopWordRank(int stopWordRank) {
        this.stopWordRank = stopWordRank;
    }

    public void setMaxTargetRank(int maxTargetRank) {
        this.maxTargetRank = maxTargetRank;
    }

    public void setMaxCandidateRank(int maxCandidateRank) {
        this.maxCandidateRank = maxCandidateRank;
    }

    public Dataset generate(int numPairs) throws IOException {

        // Select a list of the most frequent words.
        List<String> frequent = new ArrayList<String>();
        Pattern p = Pattern.compile(".*[0-9].*");
        for (String word : dictionary.getFrequentUnigrams(maxCandidateRank * 3)) {
            if (!p.matcher(word).find() && !Character.isUpperCase(word.charAt(0))) {
                frequent.add(word);
                if (frequent.size() >= maxCandidateRank) break;
            }
        }
        if (frequent.size() < stopWordRank) {
            throw new IllegalArgumentException();
        }
        frequent = frequent.subList(stopWordRank, frequent.size());
        Map<String, Integer> candidates = new HashMap<String, Integer>();
        for (String word : frequent) {
            candidates.put(word, candidates.size());
        }

        // Choose a set of targets. Each one will have an entry in the dataset.
        List<String> shuffled = new ArrayList<String>(
                (frequent.size() > maxTargetRank) ? frequent.subList(0, maxTargetRank) : frequent);
        Collections.shuffle(shuffled);
        Set<String> targets = new HashSet<String>(
                candidates.size() <= numPairs ? shuffled : shuffled.subList(0, numPairs));

        // Calculate cooccurrence counts.
        Map<String, int[]> cocounts = new HashMap<String, int[]>();
        for (String word : targets) cocounts.put(word, new int[candidates.size()]);
        Set<String> foundTargets = new HashSet<String>();
        for (WbCorpusLineReader.Line line : new WbCorpusLineReader(path)) {
            String tokens[] = line.getLine().split("\\s+");
            foundTargets.clear();
            for (int i = 0; i < tokens.length; i++) {
                if (targets.contains(tokens[i])) {
                    foundTargets.add(tokens[i]);
                }
            }
            if (foundTargets.isEmpty()) continue;
            for (String target : foundTargets) {
                for (String word : tokens) {
                    if (candidates.containsKey(word)) {
                        int i = candidates.get(word);
                        cocounts.get(target)[i] += 1;
                    }
                }
            }
        }

        // calculate pointwise mutual information
        shuffled.clear();
        shuffled.addAll(cocounts.keySet());
        Collections.shuffle(shuffled);
        List<KnownSim> pairs = new ArrayList<KnownSim>();
        double base = Math.pow(frequent.size() / 3, 1.0 / numPairs);

        for (int i = 0; i < shuffled.size(); i++) {
            String target = shuffled.get(i);
            int actual[] = cocounts.get(target);
            final double pmi[] = new double[actual.length];
            Scoreboard<String> board = new Scoreboard<String>(10);
            int n1 = dictionary.getUnigramCount(target);
            for (int j = 0; j < frequent.size(); j++) {
                int n2 = dictionary.getUnigramCount(frequent.get(j));
                pmi[j] = Math.log(actual[j] / ((n1 + 5.0) * (n2 + 5.0)));
                board.add(frequent.get(j), pmi[j]);
            }
            Integer range[] = new Integer[frequent.size()];
            for (int j = 0; j < range.length; j++) {
                range[j] = j;
            }
            Arrays.sort(range, new Comparator<Integer>() {
                @Override
                public int compare(Integer i, Integer j) {
                    return -1 * new Double(pmi[i]).compareTo(pmi[j]);
                }
            });
            int index = (int)Math.round(Math.pow(base, i));
            String choice = frequent.get(range[index]);
            double percentile = 1.0 - 1.0 * i /shuffled.size();
            pairs.add(new KnownSim(target.replace('_', ' '), choice.replace('_', ' '), percentile, lang));
        }

        return new Dataset("fake", lang, pairs);
    }
}
