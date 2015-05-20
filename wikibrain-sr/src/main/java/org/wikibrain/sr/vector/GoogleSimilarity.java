package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.matrix.MatrixRow;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.matrix.SparseMatrixRow;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.utils.SimUtils;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Google Similarity measure, as described in Milne and Witten:
 *
 * http://www.cs.waikato.ac.nz/~ihw/papers/08-DM-IHW-Semantic_relatedness.pdf
 *
 * @author Shilad Sen
 */
public class GoogleSimilarity implements VectorSimilarity {

    private static final Logger LOG = LoggerFactory.getLogger(CosineSimilarity.class);

    private TIntIntMap lengths = new TIntIntHashMap();   // lengths of each row
    private TIntSet idsInResults = new TIntHashSet();
    private final int numPages;

    private SparseMatrix features;
    private SparseMatrix transpose;

    public GoogleSimilarity(int numPages) {
        this.numPages = numPages;
    }

    @Override
    public synchronized  void setMatrices(SparseMatrix features, SparseMatrix transpose, File dataDir) throws IOException {
        this.features = features;
        this.transpose = transpose;

        File idCacheFile = new File(dataDir, "googleSimilarity-ids.bin");
        File lengthCacheFile = new File(dataDir, "googleSimilarity-lengths.bin");

        if (lengthCacheFile.exists() && lengthCacheFile.lastModified() >= features.lastModified()
        &&  idCacheFile.exists() && idCacheFile.lastModified() >= transpose.lastModified()) {
            LOG.info("reading matrix information from cache");
            lengths = (TIntIntMap) WpIOUtils.readObjectFromFile(lengthCacheFile);
            idsInResults = (TIntSet) WpIOUtils.readObjectFromFile(idCacheFile);
        } else {
            LOG.info("building cached matrix information");
            lengths.clear();
            idsInResults.clear();
            for (SparseMatrixRow row : features) {
                lengths.put(row.getRowIndex(), row.getNumCols());
            }
            idsInResults.addAll(transpose.getRowIds());
            WpIOUtils.writeObjectToFile(lengthCacheFile, lengths);
            WpIOUtils.writeObjectToFile(idCacheFile, idsInResults);
        }
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
    public double similarity(MatrixRow a, MatrixRow b) {
        int na = a.getNumCols();
        int nb = b.getNumCols();
        int intersect = 0;
        int i = 0, j = 0;

        if((na == 0 || nb == 0)) { // do not perform calculations if one or both are 0
            return 0;
        }

        // Start by getting the first column in each matrix
        int ca = a.getColIndex(i);
        int cb = b.getColIndex(j);

        while (i < na && j < nb) {
            if (ca < cb) {
                // if matrix a has a lower value, then get the next column
                i++;
                ca = a.getColIndex(i);
            } else if (ca > cb) {
                // if matrix b has a lower value, then get the next column
                j++;
                cb = b.getColIndex(j);
            } else {
                // if both have the same value, increment the intersection and get the next columns in both matrices
                i++;
                j++;
                intersect++;
                ca = a.getColIndex(i);
                cb = b.getColIndex(j);
            }
        }

        return SimUtils.googleSimilarity(na, nb, intersect, numPages);
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
        return leaderboard.getTop();
    }

    @Override
    public double getMinValue() {
        return -1.0;
    }

    @Override
    public double getMaxValue() {
        return 1.0;
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
