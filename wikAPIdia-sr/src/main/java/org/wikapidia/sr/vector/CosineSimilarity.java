package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.matrix.MatrixRow;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.sr.utils.SimUtils;

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
    public double similarity(TIntFloatMap vector1, TIntFloatMap vector2) {
        return SimUtils.cosineSimilarity(vector1, vector2);
    }

    @Override
    public SRResultList mostSimilar(TIntFloatMap query, int maxResults, TIntSet validIds) throws IOException {
        TIntDoubleHashMap dots = new TIntDoubleHashMap();
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

    public static class Provider extends org.wikapidia.conf.Provider<VectorSimilarity> {
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
