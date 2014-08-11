package org.wikibrain.sr.pairwise;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.matrix.*;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import static org.wikibrain.utils.WbMathUtils.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class KMeansDictionaryLearner {
    private static final Logger LOG = Logger.getLogger(KMeansDictionaryLearner.class.getName());

    private final Random random = new Random();
    private final Env env;
    private final Language language;
    private final LocalPageDao pageDao;
    private final SRMetric metric;

    private final int candidates[];

    private final int k1;
    private final int k2;
    private int iteration = 0;

    private double [][] centroids1;
    private double [][] centroids2;

    private double [][] nextCentroids1;
    private double [][] nextCentroids2;

    private int [] clusterCounts1;
    private int [] nextClusterCounts1;
    private int [] clusterCounts2;
    private int [] nextClusterCounts2;

    private double overallSim1 = 0.0;
    private double overallSim2 = 0.0;

    private DenseMatrix candidateCosims;

    public KMeansDictionaryLearner(Env env, SRMetric metric, final TIntSet candidateSet, int k1, int k2) throws ConfigurationException, DaoException, IOException {
        if (candidateSet.size() <= k1 * k2) {
            throw new IllegalArgumentException();
        }

        this.env = env;
        this.metric = metric;
        this.language = metric.getLanguage();
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.candidates = candidateSet.toArray();
        this.k1 = k1;
        this.k2 = k2;

        // Initialize initial clusters
        centroids1 = new double[k1][candidates.length];
        nextCentroids1 = new double[k1][candidates.length];
        clusterCounts1 = new int[k1];
        nextClusterCounts1 = new int[k1];

        // Initialize secondary clusters
        centroids2 = new double[k1*k2][candidates.length];
        nextCentroids2 = new double[k1*k2][candidates.length];
        clusterCounts2 = new int[k1*k2];
        nextClusterCounts2 = new int[k1*k2];

        Arrays.sort(candidates);

//        File tmp = File.createTempFile("cosim", "matrix");
//        tmp.delete();
//        tmp.deleteOnExit();

        File tmp = new File("test.matrix");
//        writeCosimilarityMatrix(candidateSet, tmp);

        candidateCosims = new DenseMatrix(tmp);
    }



    public void cluster() throws DaoException {
        LOG.info("Initializing clusters");
        initializeClusters();

        for (iteration = 0; iteration < 10; iteration++) {
            LOG.info("performing iteration " + iteration);
            resetDatastructures();

            ParallelForEach.iterate(getCosimIterable(true).iterator(), new Procedure<CandidateCosim>() {
                @Override
                public void call(CandidateCosim cc) throws Exception {
                    try {
                        placeCandidate(cc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            finalizeDatastructures();

            System.err.format("Mean sim 1 is %.3f, counts are %s\n", overallSim1 / candidates.length, Arrays.toString(clusterCounts1));
            System.err.format("Mean sim 2 is %.3f, counts are %s\n", overallSim2 / candidates.length, Arrays.toString(clusterCounts2));
            describeCentroids();
        }
    }

    /**
     * Randomly assigns candidates to clusters.
     */
    private void initializeClusters() {
        for (CandidateCosim cc : getCosimIterable(true)) {
            int c1 = random.nextInt(k1);
            int c2 = k2 * c1 + random.nextInt(k2);
            increment(centroids1[c1], cc.cosims);
            increment(centroids2[c2], cc.cosims);
            clusterCounts1[c1]++;
            clusterCounts2[c2]++;
        }
        for (double [] centroid : centroids1) {
            normalize(centroid);
        }
        for (double [] centroid : centroids2) {
            normalize(centroid);
        }
    }

    /**
     * Resets accumulators at the beginning of an iteration.
     */
    private void resetDatastructures() {
        Arrays.fill(nextClusterCounts1, 0);
        Arrays.fill(nextClusterCounts2, 0);
        overallSim1 = 0.0;
        zero(nextCentroids1);
        zero(nextCentroids2);
    }

    /**
     * Finalizes accumulators at the end of an iteration.
     */
    private void finalizeDatastructures() {
        copyTo(nextCentroids1, centroids1);
        copyTo(nextCentroids2, centroids2);
        copyTo(nextClusterCounts2, clusterCounts2);
        copyTo(nextClusterCounts1, clusterCounts1);

        for (double [] centroid : centroids1) {
            normalize(centroid);
        }
        for (double [] centroid : centroids2) {
            normalize(centroid);
        }
    }

    private void describeCentroids() {
        for (int i = 0; i < centroids1.length; i++) {
            Leaderboard top = new Leaderboard(5);
            for (int j = 0; j < candidates.length; j++) {
                top.tallyScore(candidates[j], centroids1[i][j]);
            }
            System.out.format("Results for cluster %d (n=%d)\n", i, clusterCounts1[i]);
            for (SRResult r : top.getTop()) {
                try {
                    LocalPage lp = pageDao.getById(language, r.getId());
                    System.out.format("\t%.3f %s\n", r.getScore(), lp.toString());
                } catch (DaoException e) {
                    throw new RuntimeException("Unexpected DAO failure!");
                }
            }
            if (iteration <= 7) {
                continue;
            }
            for (int j = 0; j < k2; j++) {
                top = new Leaderboard(3);
                int c2 = i * k2 + j;
                System.out.format("Subcluster %d (n=%d)\n", c2, clusterCounts2[c2]);
                for (int k = 0; k < candidates.length; k++) {
                    top.tallyScore(candidates[k], centroids2[c2][k]);
                }
                for (SRResult r : top.getTop()) {
                    try {
                        LocalPage lp = pageDao.getById(language, r.getId());
                        System.out.format("\t\t%.3f %s\n", r.getScore(), lp.toString());
                    } catch (DaoException e) {
                        throw new RuntimeException("Unexpected DAO failure!");
                    }
                }
            }
        }
    }

    private void placeCandidate(CandidateCosim cc) {
        int bestCluster = -1;
        double bestScore = -1.0;
        for (int i = 0; i < k1; i++) {
            double s = dot(centroids1[i], cc.cosims);
            if (s > bestScore) {
                bestScore = s;
                bestCluster = i;
            }
        }
        if (bestCluster < 0) {
            throw new IllegalStateException();
        }
        synchronized (nextCentroids1) {
            increment(nextCentroids1[bestCluster], cc.cosims);
            nextClusterCounts1[bestCluster]++;
            overallSim1 += bestScore;
        }

        int c1 = bestCluster;

        if (iteration == 1) {
            // initialize second-level clusters
            synchronized (nextCentroids2) {
                int c2 = k2 * c1 + random.nextInt(k2);
                increment(nextCentroids2[c2], cc.cosims);
                clusterCounts2[c2]++;
            }
        } else if (iteration > 3) {
            bestCluster = -1;
            bestScore = -1.0;
            for (int i = 0; i < k2; i++) {
                double s = dot(centroids2[c1 * k2 + i], cc.cosims);
                if (s > bestScore) {
                    bestScore = s;
                    bestCluster = i;
                }
            }
            synchronized (nextCentroids2) {
                increment(nextCentroids2[c1 * k2 + bestCluster], cc.cosims);
                nextClusterCounts2[c1 * k2 + bestCluster]++;
                overallSim2 += bestScore;
            }
        }
        if (cc.candidateIndex % 1000 == 0) {
            LOG.info("placing candidate " + cc.candidateIndex + " of " + candidates.length);
        }
    }

    private int[] selectDictionary(int n) {
        int multiplier = Math.max(1, n / (k1 * k2)) * 2;

        TIntDoubleMap scores = selectCandidates(centroids1, clusterCounts1, k1 * multiplier * 4);
        for (int i = 0; i < k2; i++) {
            double centroids[][] = Arrays.copyOfRange(centroids2, i  * k2, (i+1)*k2);
            int [] counts = Arrays.copyOfRange(clusterCounts2, i * k2, (i+1) * k2);
            TIntDoubleMap cscores = selectCandidates(centroids, counts, multiplier);
            for (int id : cscores.keys()) {
                scores.adjustOrPutValue(id, cscores.get(id), cscores.get(id));
            }
        }

        Leaderboard board = new Leaderboard(n);
        for (int id : scores.keys()) {
            board.tallyScore(id, scores.get(id));
        }
        int dictionary[] =  board.getTop().getIds();
        for (int id : dictionary) {
            try {
                System.out.format("dictionary: %s\n", pageDao.getById(language, id).getTitle().getCanonicalTitle());
            } catch (DaoException e) {
                throw new RuntimeException(e);
            }
        }
        return dictionary;
    }

    private TIntDoubleMap selectCandidates(double[][] centroids, int counts[], int n) {
        // Calculate entropies
        double sums[] = new double[candidates.length];
        for (double [] centroid : centroids) {
            increment(sums, centroid);
        }

        double entropies[] = new double[candidates.length];
        for (double [] centroid : centroids) {
            for (int i = 0; i < candidates.length; i++) {
                double p = centroid[i] / sums[i];
                if (p > 0) {
                    entropies[i] += -p * Math.log(p);
                }
            }
        }

        // Calculate the most discriminative item for each centroid
        Map<Integer, CandidateScore> scores = new HashMap<Integer, CandidateScore>();
        for (int i = 0; i < centroids.length; i++) {
            double centroid[] = centroids[i];
            Leaderboard board = new Leaderboard(5);
            for (int j = 0; j < candidates.length; j++) {
                double s = Math.log(1 + Math.log(counts[i] + 1)) * centroid[j] / (entropies[j] + 0.1);
                board.tallyScore(candidates[j], s);
            }
            SRResultList top = board.getTop();
            top.sortDescending();
            for (int j = 0; j < top.numDocs(); j++) {
                int id = top.getId(j);
                if (!scores.containsKey(id)) {
                    scores.put(id, new CandidateScore(id));
                }
                scores.get(id).observe(j, top.getScore(j));
            }
        }

        List<CandidateScore> sortedScores = new ArrayList<CandidateScore>(scores.values());
        Collections.sort(sortedScores);

        TIntDoubleMap result = new TIntDoubleHashMap();
        for (int i = 0; i < sortedScores.size() && i < n; i++) {
            result.put(sortedScores.get(i).id, sortedScores.get(i).score);
        }
        return result;
    }

    class CandidateScore implements Comparable<CandidateScore> {
        int id;
        int minRank = 100000;
        double score;

        public CandidateScore(int id) {
            this.id = id;
        }

        public void observe(int rank, double score) {
            minRank = Math.min(rank, minRank);
            this.score += score;
        }

        @Override
        public int compareTo(CandidateScore o) {
            if (minRank < o.minRank) {
                return -1;
            } else if (minRank > o.minRank) {
                return +1;
            } else if (score > o.score) {
                return -1;
            } else if (score < o.score) {
                return +1;
            } else {
                return id - o.id;
            }
        }
    }

    /**
     * Uses the matching pursuit algorithm to cluster optimal coefficients.
     * @param pageId
     * @return
     */
    private TIntDoubleMap selectPageCoefficients(int pageId) throws DaoException {
        /*double signal[] = getCosimilarities(pageId);
        checkForNans(signal);
        TIntDoubleMap coeffs = new TIntDoubleHashMap(maxNonZero);
        while (coeffs.size() < maxNonZero) {
            int bestIndex = -1;
            double bestSim = 0.0;
            for (int i = 0; i < dictionary.length; i++) {
                if (dictionary[i] == pageId || coeffs.containsKey(dictionary[i])) {
                    continue;
                }
                checkForNans(signal);
                checkForNans(dictionaryCosims[i]);
                double sim = dot(signal, dictionaryCosims[i]);
                checkForNan(sim);
                if (bestIndex < 0 || Math.abs(sim) > Math.abs(bestSim)) {
                    bestSim = sim;
                    bestIndex = i;
                }
            }
            if (bestIndex < 0) {
                throw new IllegalStateException();
            }
            coeffs.put(candidates[bestIndex], bestSim);
            for (int i = 0; i < signal.length; i++) {
                signal[i] -= bestSim * dictionaryCosims[bestIndex][i];
            }
            checkForNans(signal);
        }
        return coeffs;*/
        return null;
    }

    private void checkForNans(double []v) {
        for (double x : v) {
            if (Double.isInfinite(x)) throw new IllegalStateException();
            if (Double.isNaN(x)) throw new IllegalStateException();
        }
    }


    private void checkForNan(double x) {
        if (Double.isInfinite(x)) throw new IllegalStateException();
        if (Double.isNaN(x)) throw new IllegalStateException();
    }

    /**
     * From http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
     * Implementing Fisher–Yates shuffle
     */
    private void shuffle(int[] ar) {
        for (int i = ar.length - 1; i > 0; i--)  {
            int index = random.nextInt(i + 1);
            // Simple swap
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public static class CandidateCosim {
        public int candidateId;
        public int candidateIndex;
        public float cosims[];

        public CandidateCosim() {}
    }

    public Iterable<CandidateCosim> getCosimIterable(final boolean normalize) {
        return new Iterable<CandidateCosim>() {
            @Override
            public Iterator<CandidateCosim> iterator() {
                return new Iterator<CandidateCosim>() {
                    AtomicInteger i = new AtomicInteger();
                    Iterator<DenseMatrixRow> iter = candidateCosims.iterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public synchronized CandidateCosim next() {
                        CandidateCosim cc = new CandidateCosim();
                        cc.candidateId = candidates[i.get()];
                        cc.candidateIndex = i.get();
                        cc.cosims = iter.next().getValues();
                        if (normalize) normalize(cc.cosims);
                        i.incrementAndGet();
                        return cc;
                    }

                    @Override
                    public void remove() {  throw new UnsupportedOperationException(); }
                };
            }
        };
    }

    private void copyTo(double src[], double dest []) {
        System.arraycopy(src, 0, dest, 0, dest.length);
    }
    private void copyTo(int src[], int dest []) {
        System.arraycopy(src, 0, dest, 0, dest.length);
    }

    private void copyTo(double src[][], double dest [][]) {
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, dest[i].length);
        }
    }

    private void writeCosimilarityMatrix(final TIntSet candidateSet, File tmp) throws IOException {
        ValueConf vconf = new ValueConf();
        final DenseMatrixWriter writer = new DenseMatrixWriter(tmp, vconf);
        ParallelForEach.range(0, candidates.length, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                writeCosimilarities(writer, candidateSet, i);
            }
        });
        writer.finish();
    }

    private void writeCosimilarities(DenseMatrixWriter writer, TIntSet candidateSet, int i) throws DaoException, IOException {
        SRResultList mostSimilar = metric.mostSimilar(candidates[i], candidates.length / 10, candidateSet);
        mostSimilar.sortDescending();
        if (i % 1000 == 0) {
            LOG.info("building cosimilarity for " + i + " out of " + candidates.length + " with id " + candidates[i]
                            + " with min score " + mostSimilar.minScore()
            );
        }
        double scores[] = new double[candidates.length];
        Arrays.fill(scores, mostSimilar.minScore() * 0.7);
        mostSimilar.sortById();
        int k = 0;
        for (int j = 0; j < mostSimilar.numDocs(); j++) {
            int id = mostSimilar.getId(j);
            while (candidates[k] < id) { k++; }
            if (candidates[k] != id) {
                throw new IllegalStateException();
            }
            scores[k] = mostSimilar.getScore(j);
        }
        writer.writeRow(
                new DenseMatrixRow(
                        writer.getValueConf(),
                        candidates[i],
                        candidates,
                        double2Float(scores)));

    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        Language language = env.getLanguages().getDefaultLanguage();
        SRMetric metric = env.getConfigurator().get(SRMetric.class, "milnewitten", "language", language.getLangCode());

        TIntSet concepts = new TIntHashSet();
        String conceptPath = env.getConfiguration().getString("sr.concepts.path")
                + "/" + language.getLangCode() + ".txt";
        for (String line : FileUtils.readLines(new File(conceptPath))) {
            concepts.add(Integer.valueOf(line.trim()));
        }

        KMeansDictionaryLearner selector = new KMeansDictionaryLearner(env, metric, concepts, 40, 10);
        selector.cluster();

        StringBuffer contents = new StringBuffer();
        for (int id : selector.selectDictionary(50)) {
            contents.append("" + id + "\n");
        }
        FileUtils.write(new File("dictionary_ids.txt"), contents);
    }
}
