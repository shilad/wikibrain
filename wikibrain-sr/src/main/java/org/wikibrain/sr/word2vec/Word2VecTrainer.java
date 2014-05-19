package org.wikibrain.sr.word2vec;

import gnu.trove.TCollections;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
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
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 *
 * Heavily adapted from https://github.com/piskvorky/gensim/blob/develop/gensim/models/word2vec.py
 */
public class Word2VecTrainer {
    private static final Logger LOG = Logger.getLogger(Word2VecTrainer.class.getName());
    private static final int MAX_EXP = 6;
    private static final int EXP_TABLE_SIZE = 1000;

    private final Language language;
    private final RawPageDao dao;

    // These are actually indexes and counts of hashes of words.
    private final TLongIntMap wordIndexes = new TLongIntHashMap();
    final TLongIntMap wordCounts = TCollections.synchronizedMap(new TLongIntHashMap());
    private TLongObjectMap<String> hashToWords = TCollections.synchronizedMap(new TLongObjectHashMap<String>());

    // Total number of words, counting repeats many times
    private long totalWords;


    /**
     * Minimum word frequency for it to be included in the model.
     */
    private int minWordFrequency = 5;

    /**
     * Minimum length of a word for it to be included in the model.
     */
    private int minWordLength = 0;


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
    private String[] words;


    public Word2VecTrainer(RawPageDao dao, Language language) {
        this.dao = dao;
        this.language = language;
    }

    public void train(File directory) throws IOException {
        LOG.info("counting word frequencies.");
        readWords(directory);
        buildTree();

        syn0 = new float[wordIndexes.size()][layer1Size];
        for (float[] row :syn0) {
            for (int i = 0; i < row.length; i++) {
                row[i] = (random.nextFloat() - 0.5f) / layer1Size;
            }
        }
        syn1 = new float[wordIndexes.size()][layer1Size];

        LineIterator iterator = FileUtils.lineIterator(new File(directory, "hashCorpus.txt"));
        ParallelForEach.iterate(iterator,
                WpThreadUtils.getMaxThreads(),
                1000,
                new Procedure<String>() {
                    @Override
                    public void call(String sentence) throws Exception {
                        int n = trainSentence(sentence);
                        wordsTrainedSoFar.addAndGet(n);

                        // update the learning rate
                        alpha = Math.max(
                                startingAlpha * (1 - wordsTrainedSoFar.get() / (totalWords + 1.0)),
                                startingAlpha * 0.0001);
                    }
                },
                10000);
        iterator.close();


    }

    public void readWords(File directory) throws IOException {
        wordCounts.clear();
        hashToWords.clear();
        totalWords = 0;

        LOG.info("reading word counts");
        BufferedReader reader = WpIOUtils.openBufferedReader(new File(directory, "counts.txt"));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.trim().split(" ");
            long hash = Long.valueOf(tokens[0]);
            int count = Integer.valueOf(tokens[1]);
            int length = Integer.valueOf(tokens[2]);
            if (length >=minWordLength && count >= minWordFrequency) {
                wordCounts.put(hash, count);
            }
            totalWords += count;
        }
        reader.close();
        LOG.info("retained " + wordIndexes.size() + " words");


        Long[] hashes = new Long[wordCounts.size()];
        int i = 0;
        for  (long h2 : wordCounts.keys()) {
            hashes[i++] = h2;
        }
        Arrays.sort(hashes, new Comparator<Long>(){
            @Override
            public int compare(Long h1, Long h2) {
                return wordCounts.get(h2) - wordCounts.get(h1);
            }
        });
        if (i != wordCounts.size())
            throw new IllegalStateException();
        for (long h2 : hashes) {
            wordIndexes.put(h2, wordIndexes.size());
        }

        LOG.info("building hash to word mapping");
        words = new String[wordIndexes.size()];
        reader = WpIOUtils.openBufferedReader(new File(directory, "words.txt"));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.trim().split(" ", 2);
            long hash = Long.valueOf(tokens[0]);
            if (wordIndexes.containsKey(hash)) {
                words[wordIndexes.get(hash)] = tokens[1];
                hashToWords.put(Long.valueOf(tokens[0]), tokens[1]);
            }
        }
    }

    private int trainSentence(String sentence) {
        String hashStrs[] = sentence.trim().split(" ");
        int indexes[] = new int[hashStrs.length];
        for (int i = 0; i < hashStrs.length; i++) {
            indexes[i] = -1;
            if (hashStrs[i].length() > 0) {
                long h = Long.valueOf(hashStrs[i]);
                if (wordIndexes.containsKey(h)) {
                    indexes[i] = wordIndexes.get(h);
                }
            }
        }

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
                if (i == j || indexes[j] < 0) {
                    continue; // skip the word itself and out of vocabulary words
                }
                Arrays.fill(neu1e, 0f);
                float l1[] = syn0[indexes[j]];

                for (int k = 0; k < parents.length; k++) {
                    float l2[] = syn1[parents[k]];
                    double f = MathUtils.dot(l1, l2);
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
        List<String> words = new ArrayList(Arrays.asList(this.hashToWords.values()));
        Collections.sort(words, new Comparator<String>() {
            @Override
            public int compare(String w1, String w2) {
                long h1 = Word2VecUtils.hashWord(w1);
                long h2 = Word2VecUtils.hashWord(w2);
                return wordCounts.get(h2) - wordCounts.get(h1);
            }
        });

        OutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        stream.write((words.size() + " " + layer1Size + "\n").getBytes());
        for (String w : words) {
            stream.write(w.getBytes("UTF-8"));
            stream.write(' ');
            float[] vector = syn0[wordIndexes.get(Word2VecUtils.hashWord(w))];
            MathUtils.normalize(vector);
            for (float f : vector) {
                stream.write(floatToBytes(f));
            }
        }
    }

    private void test() {
        long h = 0;
        for (long hash : hashToWords.keys()) {
            if (hashToWords.get(hash).equals("person")) {
                h = hash;
                break;
            }
        }
        if (h == 0) {
            throw new NoSuchElementException();
        }
        float [] v1 = syn0[wordIndexes.get(h)];
        MathUtils.normalize(v1);

        Map<String, Double> sims = new HashMap<String, Double>();
        for (long h2 : wordIndexes.keys()) {
            float [] v2 = syn0[wordIndexes.get(h2)];
            MathUtils.normalize(v2);
            double sim =  MathUtils.dot(v1, v2);
            sims.put(hashToWords.get(h2), sim);
        }

        List<String> keys = new ArrayList<String>(sims.keySet());
        Collections.sort(keys, new MapValueComparator(sims, false));
        keys = keys.subList(0, 100);
        for (String k : keys) {
            System.out.println(sims.get(k) + " " + k);
        }
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
                c.get(RawPageDao.class),
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
