package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.matrix.MatrixRow;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.matrix.SparseMatrixRow;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.utils.SimUtils;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class CosineSimilarity implements VectorSimilarity {
    private static final Logger LOG = Logger.getLogger(CosineSimilarity.class.getName());

    private final TIntFloatHashMap lengths = new TIntFloatHashMap();   // lengths of each row
    private final TIntSet idsInResults = new TIntHashSet();
    private int maxResults = -1;

    private SparseMatrix features;
    private SparseMatrix transpose;

    @Override
    public synchronized  void setMatrices(SparseMatrix features, SparseMatrix transpose) {
        this.features = features;
        this.transpose = transpose;

        LOG.info("building cached matrix information");
        lengths.clear();
        idsInResults.clear();
        maxResults = 0;
        for (SparseMatrixRow row : features) {
            lengths.put(row.getRowIndex(), (float) row.getNorm());
            maxResults = Math.max(maxResults, row.getNumCols());
        }
        idsInResults.addAll(transpose.getRowIds());
    }

    @Override
    public double similarity(MatrixRow a, MatrixRow b) {
        double adota = 0.0;
        double bdotb = 0.0;
        double adotb = 0.0;

        int na = a.getNumCols();
        int nb = b.getNumCols();
        int i = 0, j = 0;

        while (i < na && j < nb) {
            int ca = a.getColIndex(i);
            int cb = b.getColIndex(j);
            if (ca < cb) {
                float va = a.getColValue(i++);
                adota += va * va;
            } else if (ca > cb) {
                float vb = b.getColValue(j++);
                bdotb += vb * vb;
            } else {
                float va = a.getColValue(i++);
                float vb = b.getColValue(j++);
                adota += va * va;
                bdotb += vb * vb;
                adotb += va * vb;
            }
        }

        for (; i < na; i++) {
            float va = a.getColValue(i);
            adota += va * va;
        }
        for (; j < nb; j++) {
            float vb = b.getColValue(j);
            bdotb += vb * vb;
        }

        if (adota * bdotb * adotb == 0) {
            return 0.0;
        } else {
            return adotb / Math.sqrt(adota * bdotb);
        }
    }

    @Override
    public double similarity(TIntFloatMap vector1, TIntFloatMap vector2) {
        return SimUtils.cosineSimilarity(vector1, vector2);
    }

    @Override
    public SRResultList mostSimilar(TIntFloatMap query, int maxResults, TIntSet validIds) throws IOException {
        if (validIds != null && validIds.size() < 10000) {
            return mostSimilarWithRegularIndex(query, maxResults, validIds);
        } else {
            return mostSimilarWithInvertedIndex(query, maxResults, validIds);
        }
    }

    private SRResultList mostSimilarWithRegularIndex(TIntFloatMap query, int maxResults, TIntSet validIds) throws IOException {
        final Leaderboard leaderboard = new Leaderboard(maxResults);
        double rowNorm = norm(query);

        for (int id : validIds.toArray()) {
            MatrixRow row2 = features.getRow(id);
            if (row2 != null) {
                double dot = 0.0;
                for (int i = 0; i < row2.getNumCols(); i++) {
                    int id2 = row2.getColIndex(i);
                    float val2 = query.get(id2);
                    if (val2 > 0) {
                        dot += val2 + row2.getColValue(i);
                    }
                }
                double l1 = lengths.get(id);
                double l2 = rowNorm;
                double sim = dot / (l1 * l2);
                leaderboard.tallyScore(id, sim);
            }
        }

        SRResultList result = leaderboard.getTop();
        result.sortDescending();
        return result;
    }

    private SRResultList mostSimilarWithInvertedIndex(TIntFloatMap query, int maxResults, TIntSet validIds) throws IOException {
        TIntDoubleHashMap dots = new TIntDoubleHashMap(maxResults * 5);
        for (int id : query.keys()) {
            float val1 = query.get(id);
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
        double rowNorm = norm(query);
        for (int id : dots.keys()) {
            double l1 = lengths.get(id);
            double l2 = rowNorm;
            double dot = dots.get(id);
            double sim = dot / (l1 * l2);
            leaderboard.tallyScore(id, sim);
        }

        SRResultList result = leaderboard.getTop();
        result.sortDescending();
        return result;
    }

    @Override
    public double getMinValue() {
        return -1.0;
    }

    @Override
    public double getMaxValue() {
        return 1.0;
    }

    private double norm(TIntFloatMap vector) {
        double length = 0;
        for (float x : vector.values()) {
            length += x * x;
        }
        return Math.sqrt(length);
    }

    public static class Provider extends org.wikibrain.conf.Provider<VectorSimilarity> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return VectorSimilarity.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.similarity";
        }

        @Override
        public VectorSimilarity get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("cosine")) {
                return null;
            }
            return new CosineSimilarity();
        }
}
}
