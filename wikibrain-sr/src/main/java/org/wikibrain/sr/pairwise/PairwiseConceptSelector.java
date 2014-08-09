package org.wikibrain.sr.pairwise;

import gnu.trove.map.TIntDoubleMap;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class PairwiseConceptSelector {
    private static final Logger LOG = Logger.getLogger(PairwiseConceptSelector.class.getName());

    private final Random random = new Random();
    private final Env env;
    private final Language language;
    private final LocalPageDao pageDao;
    private final SRMetric metric;

    private final int candidates[];

    private final int k1;
    private final int k2;

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

    public PairwiseConceptSelector(Env env, SRMetric metric, final TIntSet candidateSet, int k1, int k2) throws ConfigurationException, DaoException, IOException {
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
        nextCentroids1 = new double[k1*k2][candidates.length];
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



    public TIntDoubleMap cluster() throws DaoException {
        LOG.info("Initializing clusters");
        initializeClusters();

        for (int i = 0; i < 200; i++) {
            LOG.info("performing iteration " + i);
            resetDatastructures();

            ParallelForEach.iterate(getCosimIterable(true).iterator(), new Procedure<CandidateCosim>() {
                @Override
                public void call(CandidateCosim cc) throws Exception {
                    placeCandidate(cc);
                }
            });

            finalizeDatastructures();

            System.err.format("Mean sim is %.3f, counts are %s\n", overallSim1 / candidates.length, Arrays.toString(clusterCounts1));
            describeCentroids();
        }
        return null;
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
        }
    }

    private void placeCandidate(CandidateCosim cc) {
        int bestCluster = -1;
        double bestScore = -1.0;
        for (int k = 0; k < k1; k++) {
            double s = dot(centroids1[k], cc.cosims);
            if (s > bestScore) {
                bestScore = s;
                bestCluster = k;
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
        if (cc.candidateIndex % 1000 == 0) {
            LOG.info("placing candidate " + cc.candidateIndex + " of " + candidates.length + " with best " + bestScore);
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

        PairwiseConceptSelector selector = new PairwiseConceptSelector(env, metric, concepts, 20, 20);
        selector.cluster();
    }
}
