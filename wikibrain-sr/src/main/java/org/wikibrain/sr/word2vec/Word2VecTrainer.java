package org.wikibrain.sr.word2vec;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.nlp.Dictionary;
import org.wikibrain.sr.wikify.WBCorpusDocReader;
import org.wikibrain.sr.wikify.WbCorpusLineReader;
import org.wikibrain.utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;

/**
 * @author Shilad Sen
 *
 * Heavily adapted from https://github.com/piskvorky/gensim/blob/develop/gensim/models/word2vec.py
 */
public class Word2VecTrainer {
    private static final Logger LOG = LoggerFactory.getLogger(Word2VecTrainer.class);
    private static final int ARTICLE_COUNT_BONUS = 10;
    private static final int MAX_EXP = 6;
    private static final int EXP_TABLE_SIZE = 1000;

    private final Language language;
    private final LocalPageDao pageDao;

    // These are actually indexes and counts of hashes of words.
    private final TLongIntMap wordIndexes = new TLongIntHashMap();
    private final TLongIntMap wordCounts = new TLongIntHashMap();

    // Mapping from article id to hash of string representation ("/w/en/1000/Hercule_Poirot").
    private final TIntIntMap articleIndexes = new TIntIntHashMap();

    // Total number of words, counting repeats many times
    private long totalWords;

    /**
     * Minimum word frequency for it to be included in the model.
     */
    private int minWordFrequency = 5;

    /**
     * Minimum number of times an article must be mentioned for it to be included in the model.
     */
    private int minMentionFrequency = 5;

    /**
     * Maximum number of words
     */
    private int maxWords = 5000000;


    private double startingAlpha = 0.025;
    private double alpha = startingAlpha;
    private int window = 5;

    private int layer1Size = 200;
    private float syn0[][];
    private float syn1[][];

    /**
     * Fast sigmoid function table.
     */
    private static final double[] EXP_TABLE = new double[EXP_TABLE_SIZE];
    static {
        for (int i = 0; i < EXP_TABLE_SIZE; i++) {
            EXP_TABLE[i] = Math.exp(((i / (double) EXP_TABLE_SIZE * 2 - 1) * MAX_EXP));
            EXP_TABLE[i] = EXP_TABLE[i] / (EXP_TABLE[i] + 1);
        }
    }

    private AtomicLong wordsTrainedSoFar = new AtomicLong();
    private Random random = new Random();

    private byte[][] wordCodes;
    private int[][] wordParents;
    private String[] words = null;
    private boolean keepAllArticles = false;
    private int iterations = 2;


    public Word2VecTrainer(LocalPageDao pageDao, Language language) {
        this.pageDao = pageDao;
        this.language = language;
    }

    public void train(File directory) throws IOException, DaoException {
        train(directory, true);
    }

    public void train(File directory, boolean wikibrainFormat) throws IOException, DaoException {
        LOG.info("counting word frequencies.");
        readWords(new File(directory, "dictionary.txt"));
        buildTree();

        syn0 = new float[wordIndexes.size()][layer1Size];
        for (float[] row :syn0) {
            for (int i = 0; i < row.length; i++) {
                row[i] = (random.nextFloat() - 0.5f) / layer1Size;
            }
        }
        syn1 = new float[wordIndexes.size()][layer1Size];

        for (int it = 0; it < iterations; it++) {
            if (wikibrainFormat) {
                WBCorpusDocReader reader = new WBCorpusDocReader(new File(directory, "corpus.txt"));
                ParallelForEach.iterate(reader.iterator(),
                        WpThreadUtils.getMaxThreads(),
                        1000,
                        new Procedure<WBCorpusDocReader.Doc>() {
                            @Override
                            public void call(WBCorpusDocReader.Doc doc) throws Exception {
                                int n = 0;
                                for (String line : doc.getLines()) {
                                    n += trainSentence(doc.getDoc().getId(), line);
                                }
                                wordsTrainedSoFar.addAndGet(n);

                                // update the learning rate
                                alpha = Math.max(
                                        startingAlpha * (1 - wordsTrainedSoFar.get() / (iterations * totalWords + 1.0)),
                                        startingAlpha * 0.0001);
                            }
                        },
                        10000);
            } else {
                LineIterator iterator = FileUtils.lineIterator(new File(directory, "corpus.txt"));
                ParallelForEach.iterate(iterator,
                        WpThreadUtils.getMaxThreads(),
                        1000,
                        new Procedure<String>() {
                            @Override
                            public void call(String sentence) throws Exception {
                                int n = trainSentence(null, sentence);
                                wordsTrainedSoFar.addAndGet(n);

                                // update the learning rate
                                alpha = Math.max(
                                        startingAlpha * (1 - wordsTrainedSoFar.get() / (iterations * totalWords + 1.0)),
                                        startingAlpha * 0.0001);
                            }
                        },
                        10000);
                iterator.close();
            }
        }
    }

    public void readWords(File dictionary) throws IOException, DaoException {
        LOG.info("reading word counts");
        Dictionary dict = new Dictionary(language, Dictionary.WordStorage.IN_MEMORY);
        dict.setCountBigrams(false);
        dict.setContainsMentions(true);
        dict.read(dictionary, maxWords, minWordFrequency);

        totalWords = dict.getTotalCount();
        List<String> top = dict.getFrequentUnigramsAndMentions(pageDao, maxWords, minWordFrequency, minMentionFrequency);
        for (int i = 0; i < top.size(); i++) {
            String w = top.get(i);
            long h = hashWord(w);
            wordIndexes.put(h, i);
            if (w.startsWith("/w/")) {
                int wpId = Integer.valueOf(w.split("/", 5)[3]);
                articleIndexes.put(wpId, i);
                wordCounts.put(h, dict.getMentionCount(wpId) + ARTICLE_COUNT_BONUS);
            } else {
                wordCounts.put(h, dict.getUnigramCount(w));
            }
        }
        if (keepAllArticles) {
            for (LocalPage page : pageDao.get(DaoFilter.normalPageFilter(language))) {
                if (!articleIndexes.containsKey(page.getLocalId())) {
                    String w = page.getCompactUrl();
                    long h = hashWord(w);
                    if (wordIndexes.containsKey(h)) {
                        LOG.warn("hash collision on " + w + " with hash " + h);
                    } else {
                        int i = top.size();
                        wordIndexes.put(h, i);
                        top.add(w);
                        articleIndexes.put(page.getLocalId(), i);
                        wordCounts.put(h,  ARTICLE_COUNT_BONUS);
                    }
                }
            }
        }
        words = top.toArray(new String[top.size()]);

        LOG.info("retained " + dict.getNumUnigrams() + " words and " + articleIndexes.size() + " articles");
    }

    private int trainSentence(Integer wpId, String sentence) {
        int wpIdIndex = (wpId != null && articleIndexes.containsKey(wpId)) ? articleIndexes.get(wpId) : -1;
        String words[] = sentence.trim().split(" +");
        TIntList indexList = new TIntArrayList(words.length * 3 / 2);
        for (int i = 0; i < words.length; i++) {
            int wordIndex = -1;
            int mentionIndex = -1;
            int mentionStart = words[i].indexOf(":/w/");
            if (mentionStart >= 0) {
                Matcher m = Dictionary.PATTERN_MENTION.matcher(words[i].substring(mentionStart));
                if (m.matches()) {
                    int wpId2 = Integer.valueOf(m.group(3));
                    if (articleIndexes.containsKey(wpId2)) {
                        mentionIndex = articleIndexes.get(wpId2);
                    }
                    words[i] = words[i].substring(0, mentionStart);
                }
            }
            if (words[i].length() > 0) {
                long h = hashWord(words[i]);
                if (wordIndexes.containsKey(h)) {
                    wordIndex = wordIndexes.get(h);
                }
            }
            if (mentionIndex >= 0) {
                if (random.nextDouble() >= 0.5) {
                    indexList.add(wordIndex);
                    indexList.add(mentionIndex);
                } else {
                    indexList.add(mentionIndex);
                    indexList.add(wordIndex);
                }
            } else {
                indexList.add(wordIndex);
            }
        }
        int indexes[] = indexList.toArray();

        float[] neu1e = new float[layer1Size];
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] < 0) {
                continue; // skip out of vocabulary words
            }

            byte [] code = wordCodes[indexes[i]];
            int [] parents = wordParents[indexes[i]];
            if (code.length != parents.length) {
                throw new IllegalStateException();
            }

            // now go over all words from the (reduced) window, predicting each one in turn
            int reducedWindow = random.nextInt(window);
            int start = Math.max(0, i - window + reducedWindow);
            int end = Math.min(indexes.length, i + window + 1 - reducedWindow);

            for (int j = start; j < end; j++) {
                int q;
                if (i == j) {
                    // hack: update the parent document, if it exists.
                    // Otherwise word2vec skips the word itself.
                    q = wpIdIndex;
                } else {
                    q = indexes[j];
                }
                if (q < 0) {
                    continue;
                }
                Arrays.fill(neu1e, 0f);
                float l1[] = syn0[q];

                for (int k = 0; k < parents.length; k++) {
                    float l2[] = syn1[parents[k]];
                    double f = WbMathUtils.dot(l1, l2);
                    if (f <= -MAX_EXP || f >= MAX_EXP) {
                        continue;
                    }
                    double s = EXP_TABLE[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
                    double g = (1 - code[k] - s) * alpha;

                    for (int c = 0; c < layer1Size; c++) {
                        neu1e[c] += g * l2[c];
                        l2[c] += g * l1[c];
                    }
                }
                for (int c = 0; c < layer1Size; c++) {
                    l1[c] += neu1e[c];
                }
            }

        }
        return indexes.length;
    }


    private class Node implements Comparable<Node> {
        long hash;
        int index;
        int count;
        Node left;
        Node right;

        private Node(long hash, int count, int index) {
            this.hash = hash;
            this.count = count;
            this.index = index;
        }
        private Node(long hash, int count, int index, Node left, Node right) {
            this.hash = hash;
            this.count = count;
            this.index = index;
            this.left = left;
            this.right = right;
        }
        public void setCode(byte [] code) {
            if (hash != 0) wordCodes[index] = code;
            if (left != null) left.setCode(ArrayUtils.add(code, (byte)0));
            if (right != null) right.setCode(ArrayUtils.add(code, (byte)1));
        }

        public void setPoints(int [] points) {
            if (hash != 0) wordParents[index] = points;
            points = ArrayUtils.add(points, index - wordIndexes.size());
            if (left != null) left.setPoints(points);
            if (right != null) right.setPoints(points);
        }

        public int getHeight() {
            int height = 0;
            if (left != null) height = Math.max(height, left.getHeight());
            if (right != null) height = Math.max(height, right.getHeight());
            return height + 1;
        }

        @Override
        public int compareTo(Node o) {
            return count - o.count;
        }
    }

    private void buildTree() {
        LOG.info("creating initial heap");
        PriorityQueue<Node> heap = new PriorityQueue<Node>();
        for (long hash : wordIndexes.keys()) {
            heap.add(new Node(hash, wordCounts.get(hash), wordIndexes.get(hash)));
        }

        LOG.info("creating huffman tree");
        for (int i = 0; heap.size() > 1; i++) {
            Node n1 = heap.poll();
            Node n2 = heap.poll();
            Node n = new Node(0, n1.count + n2.count, i + wordIndexes.size(), n1, n2);
            heap.add(n);
        }

        Node root = heap.poll();
        if (!heap.isEmpty()) {
            throw new IllegalStateException();
        }
        this.wordParents = new int[wordIndexes.size()][];
        this.wordCodes = new byte[wordIndexes.size()][];
        root.setPoints(new int[0]);
        root.setCode(new byte[0]);
        LOG.info("built tree of height " + root.getHeight());
    }


    public void save(File path) throws IOException {
        FileUtils.deleteQuietly(path);
        path.getParentFile().mkdirs();
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        stream.write((words.length + " " + layer1Size + "\n").getBytes());
        for (String w : words) {
            stream.write(w.getBytes("UTF-8"));
            stream.write(' ');
            float[] vector = syn0[wordIndexes.get(Word2VecUtils.hashWord(w))];
            WbMathUtils.normalize(vector);
            for (float f : vector) {
                stream.write(floatToBytes(f));
            }
        }
        stream.close();
    }

    private void test() {
        long h = hashWord("person");
        float [] v1 = syn0[wordIndexes.get(h)];
        WbMathUtils.normalize(v1);

        Map<String, Double> sims = new HashMap<String, Double>();
        for (int i = 0; i < words.length; i++) {
            float [] v2 = syn0[i];
            WbMathUtils.normalize(v2);
            double sim =  WbMathUtils.dot(v1, v2);
            sims.put(words[i], sim);
        }

        List<String> keys = new ArrayList<String>(sims.keySet());
        Collections.sort(keys, new MapValueComparator(sims, false));
        keys = keys.subList(0, 100);
        for (String k : keys) {
            System.out.println(sims.get(k) + " " + k);
        }
    }

    public void setMaxWords(int maxWords) {
        this.maxWords = maxWords;
    }

    public void setLayer1Size(int layer1Size) {
        this.layer1Size = layer1Size;
    }

    public void setWindow(int window) {
        this.window = window;
    }

    public void setKeepAllArticles(boolean keepAllArticles) {
        this.keepAllArticles = keepAllArticles;
    }

    private static byte[] floatToBytes(float value) {
        int bits = Float.floatToIntBits(value);
        byte[] bytes = new byte[4];
        bytes[0] = (byte)(bits & 0xff);
        bytes[1] = (byte)((bits >> 8) & 0xff);
        bytes[2] = (byte)((bits >> 16) & 0xff);
        bytes[3] = (byte)((bits >> 24) & 0xff);
        return bytes;
    }

    private static long hashWord(String word) {
        return Word2VecUtils.hashWord(word);
    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("output")
                        .withDescription("model output file")
                        .create("o"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .isRequired()
                        .withLongOpt("input")
                        .withDescription("corpus input directory (as generated by WikiTextCorpusCreator)")
                        .create("i"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("layer1size")
                        .withDescription("size of the layer 1 neural network")
                        .create("z"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("window")
                        .withDescription("size of the sliding window")
                        .create("w"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("minfreq")
                        .withDescription("minimum word frequency")
                        .create("f"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("WikiTextCorpusCreator", options);
            return;
        }
        Env env = new EnvBuilder(cmd).build();
        Configurator c = env.getConfigurator();
        Word2VecTrainer trainer = new Word2VecTrainer(
                c.get(LocalPageDao.class),
                env.getLanguages().getDefaultLanguage()
           );
        if (cmd.hasOption("f")) {
            trainer.minWordFrequency = Integer.valueOf(cmd.getOptionValue("f"));
        }
        if (cmd.hasOption("w")) {
            trainer.window = Integer.valueOf(cmd.getOptionValue("w"));
        }
        if (cmd.hasOption("z")) {
            trainer.layer1Size = Integer.valueOf(cmd.getOptionValue("z"));
        }

        trainer.train(new File(cmd.getOptionValue("i")));
        trainer.save(new File(cmd.getOptionValue("o")));
    }
}
