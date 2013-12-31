package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.NameSpace;
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
 * Google Similarity measure, as described in Milne and Witten:
 *
 * http://www.cs.waikato.ac.nz/~ihw/papers/08-DM-IHW-Semantic_relatedness.pdf
 *
 * @author Shilad Sen
 */
public class GoogleSimilarity implements VectorSimilarity {

    private static final Logger LOG = Logger.getLogger(CosineSimilarity.class.getName());

    private final TIntIntMap lengths = new TIntIntHashMap();   // lengths of each row
    private final TIntSet idsInResults = new TIntHashSet();
    private final int numPages;

    private SparseMatrix features;
    private SparseMatrix transpose;

    public GoogleSimilarity(int numPages) {
        this.numPages = numPages;
    }

    @Override
    public synchronized  void setMatrices(SparseMatrix features, SparseMatrix transpose) {
        this.features = features;
        this.transpose = transpose;

        LOG.info("building cached matrix information");
        lengths.clear();
        idsInResults.clear();
        for (SparseMatrixRow row : features) {
            lengths.put(row.getRowIndex(), row.getNumCols());
        }
        idsInResults.addAll(transpose.getRowIds());
        System.out.println("found " + features.getNumRows() + " and " + transpose.getNumRows());
    }

    @Override
    public double similarity(TIntFloatMap vector1, TIntFloatMap vector2) {
        if (vector2.size() < vector1.size()) {
            TIntFloatMap tmp = vector1;
            vector1 = vector2;
            vector2 = tmp;
        }
        int size1 = vector1.size();
        int size2 = vector2.size();
        if (size1 == 0 || size2 == 0) {
            return 0.0;
        }
        int intersect = 0;
        for (int id1 : vector1.keys()) {
            if (vector2.containsKey(id1)) {
                intersect++;
            }
        }
        if (intersect == 0) {
            return 0.0;
        }
        return SimUtils.googleSimilarity(size1, size2, intersect, numPages);
    }

    @Override
    public SRResultList mostSimilar(TIntFloatMap query, int maxResults, TIntSet validIds) throws IOException {
        TIntIntMap intersections = new TIntIntHashMap();
        for (int id1 : query.keys()){
            SparseMatrixRow row = transpose.getRow(id1);
            for (int i = 0; i < row.getNumCols(); i++) {
                int id2 = row.getColIndex(i);
                if (validIds == null || validIds.contains(id2)) {
                    intersections.adjustOrPutValue(id2, 1, 1);
                }
            }
        }
        Leaderboard leaderboard = new Leaderboard(maxResults);
        for (int id: intersections.keys()) {
            double sim = SimUtils.googleSimilarity(query.size(),  lengths.get(id),
                                                   intersections.get(id), numPages);
            leaderboard.tallyScore(id, sim);
        }
        SRResultList result = leaderboard.getTop();
        result.sortDescending();
        return result;
    }

    @Override
    public SRResult addExplanations(TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) {
        // TODO: implement me
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
            if (!config.getString("type").equals("google")) {
                return null;
            }
            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("GoogleSimilarity requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            LocalPageDao lpDao = getConfigurator().get(LocalPageDao.class);
            try {
                int numPages = lpDao.getCount(
                                    new DaoFilter()
                                            .setLanguages(language)
                                            .setRedirect(false)
                                            .setDisambig(false)
                                            .setNameSpaces(NameSpace.ARTICLE));

                return new GoogleSimilarity(numPages);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
