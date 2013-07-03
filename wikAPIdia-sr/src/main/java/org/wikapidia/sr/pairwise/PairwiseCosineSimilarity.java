package org.wikapidia.sr.pairwise;

import edu.macalester.wpsemsim.concepts.ConceptMapper;
import edu.macalester.wpsemsim.lucene.IndexHelper;
import edu.macalester.wpsemsim.matrix.Matrix;
import edu.macalester.wpsemsim.matrix.MatrixRow;
import edu.macalester.wpsemsim.matrix.SparseMatrix;
import edu.macalester.wpsemsim.matrix.SparseMatrixRow;
import edu.macalester.wpsemsim.sim.BaseSimilarityMetric;
import edu.macalester.wpsemsim.sim.SimilarityMetric;
import edu.macalester.wpsemsim.utils.DocScoreList;
import edu.macalester.wpsemsim.utils.Leaderboard;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.lucene.queryparser.surround.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class PairwiseCosineSimilarity extends BaseSimilarityMetric implements SimilarityMetric {
    private static final Logger LOG = Logger.getLogger(PairwiseCosineSimilarity.class.getName());

    private SparseMatrix matrix;
    private SparseMatrix transpose;
    private TIntFloatHashMap lengths = null;   // lengths of each row
    private int maxResults = -1;
    private SimilarityMetric basedOn;   // underlying similarity metric that generated these similarities
    private boolean buildPhraseVectors; // if true, build phrase vectors using the underlying similarity metric.
    private TIntSet idsInResults = new TIntHashSet();

    public PairwiseCosineSimilarity(SparseMatrix matrix, SparseMatrix transpose) throws IOException {
        this(null, null, matrix, transpose);

    }
    public PairwiseCosineSimilarity(ConceptMapper mapper, IndexHelper helper, SparseMatrix matrix, SparseMatrix transpose) throws IOException {
        super(mapper, helper);
        this.matrix = matrix;
        this.transpose = transpose;
        setName("pairwise-cosine-similarity (matrix=" +
                matrix.getPath() + ", transpose=" +
                transpose.getPath() + ")");
    }

    public void setBasedOn(SimilarityMetric metric) {
        this.basedOn = metric;
    }

    public synchronized void initIfNeeded() {
        if (lengths == null) {
            LOG.info("building cached matrix information");
            lengths = new TIntFloatHashMap();
            for (SparseMatrixRow row : matrix) {
                lengths.put(row.getRowIndex(), (float) row.getNorm());
                maxResults = Math.max(maxResults, row.getNumCols());
            }
            idsInResults.addAll(transpose.getRowIds());
        }
    }

    @Override
    public double similarity(int wpId1, int wpId2) throws IOException {
        double sim = 0;
        MatrixRow row1 = matrix.getRow(wpId1);
        if (row1 != null) {
            MatrixRow row2 = matrix.getRow(wpId2);
            if (row2 != null) {
                    sim = cosineSimilarity(row1.asTroveMap(), row2.asTroveMap());
            }
        }
        return normalize(sim);

    }

    @Override
    public double similarity(String phrase1, String phrase2) throws IOException {
        if (!buildPhraseVectors) {
            return super.similarity(phrase1, phrase2);
        }
        if (basedOn == null) {
            throw new IllegalArgumentException("basedOn must be non-null if buildPhraseVectors is true");
        }
        initIfNeeded();
        DocScoreList list1 = basedOn.mostSimilar(phrase1, maxResults, idsInResults);
        DocScoreList list2 = basedOn.mostSimilar(phrase2, maxResults, idsInResults);
        list1.makeUnitLength();
        list2.makeUnitLength();
        return normalize(cosineSimilarity(list1.asTroveMap(), list2.asTroveMap()));
    }

    @Override
    public DocScoreList mostSimilar(int wpId, int maxResults, TIntSet validIds) throws IOException {
        MatrixRow row = matrix.getRow(wpId);
        if (row == null) {
            LOG.info("unknown wpId: " + wpId);
            return new DocScoreList(0);
        }
        TIntFloatHashMap vector = row.asTroveMap();
        return mostSimilar(maxResults, validIds, vector);
    }

    @Override
    public DocScoreList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws IOException {
        if (!buildPhraseVectors) {
            return super.mostSimilar(phrase, maxResults, validIds);
        }
        if (basedOn == null) {
            throw new IllegalArgumentException("basedOn must be non-null if buildPhraseVectors is true");
        }
        initIfNeeded();
        DocScoreList list = basedOn.mostSimilar(phrase, maxResults, idsInResults);
        if (list == null) {
            return null;
        } else {
            return mostSimilar(maxResults, validIds, list.asTroveMap());
        }

    }

    private DocScoreList mostSimilar(int maxResults, TIntSet validIds, TIntFloatHashMap vector) throws IOException {
        initIfNeeded();
        TIntDoubleHashMap dots = new TIntDoubleHashMap();

        for (int id : vector.keys()) {
            float val1 = vector.get(id);
            MatrixRow row2 = transpose.getRow(id);
            if (row2 != null) {
                for (int j = 0; j < row2.getNumCols(); j++) {
                    int id2 = row2.getColIndex(j);
                    if (validIds == null || validIds.contains(id2)) {
                        float val2 = row2.getColValue(j);
                        dots.adjustOrPutValue(id2, val1 * val2, val1 * val2);
                    }
                }
            }
        }

        final Leaderboard leaderboard = new Leaderboard(maxResults);
        double rowNorm = norm(vector);
        for (int id : dots.keys()) {
            double l1 = lengths.get(id);
            double l2 = rowNorm;
            double dot = dots.get(id);
            double sim = dot / (l1 * l2);
            leaderboard.tallyScore(id, sim);
        }

        return normalize(leaderboard.getTop());
    }


    private double cosineSimilarity(TIntFloatHashMap map1, TIntFloatHashMap map2) {
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;

        for (float x: map1.values()) { xDotX += x * x; }
        for (float y: map2.values()) { yDotY += y * y; }
        for (int id : map1.keys()) {
            if (map2.containsKey(id)) {
                xDotY += map1.get(id) * map2.get(id);
            }
        }
        return xDotY / Math.sqrt(xDotX * yDotY);
    }

    private double norm(TIntFloatHashMap vector) {
        double length = 0;
        for (float x : vector.values()) {
            length += x * x;
        }
        return Math.sqrt(length);
    }

    public void setBuildPhraseVectors(boolean buildPhraseVectors) {
        this.buildPhraseVectors = buildPhraseVectors;
    }

    public static int PAGE_SIZE = 1024*1024*500;    // 500MB
    public static void main(String args[]) throws IOException, InterruptedException {
        if (args.length != 4 && args.length != 5) {
            System.err.println("usage: " + PairwiseCosineSimilarity.class.getName()
                    + " path_matrix path_matrix_transpose path_output maxResultsPerDoc [num-cores]");
            System.exit(1);
        }
        SparseMatrix matrix = new SparseMatrix(new File(args[0]), 1, PAGE_SIZE);
        SparseMatrix transpose = new SparseMatrix(new File(args[1]));
        PairwiseCosineSimilarity sim = new PairwiseCosineSimilarity(matrix, transpose);
        int cores = (args.length == 5)
                ? Integer.valueOf(args[4])
                : Runtime.getRuntime().availableProcessors();

        PairwiseSimilarityWriter writer = new PairwiseSimilarityWriter(sim, new File(args[2]));
        writer.writeSims(matrix.getRowIds(), cores, Integer.valueOf(args[3]));
    }
}
