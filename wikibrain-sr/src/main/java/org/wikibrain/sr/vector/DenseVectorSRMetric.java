package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;
import org.wikibrain.matrix.knn.KNNFinder;
import org.wikibrain.matrix.knn.Neighborhood;
import org.wikibrain.matrix.knn.RandomProjectionKNNFinder;
import org.wikibrain.sr.BaseSRMetric;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An SR metric that represents phrases and pages using dense numeric vectors.
 * SR scores are the result of cosine similarity between vectors. Word2Vec uses this
 * representations.
 *
 * <p>The metric requires a DenseVectorGenerator class that generates the vectors.</p>
 *
 * @author Shilad Sen
 * @see org.wikibrain.sr.vector.DenseVectorGenerator
 */
public class DenseVectorSRMetric extends BaseSRMetric {

    private static final Logger LOG = LoggerFactory.getLogger(DenseVectorSRMetric.class);
    protected final DenseVectorGenerator generator;
    protected final SRConfig config;

    private DenseMatrix articleFeatures;
    private KNNFinder accelerator;

    // Multiple of requested mostSimilar results that will be considered
    private double acceleratorMultiplier = 100.0;
    private int minAcceleratorCandidates = 500;


    public DenseVectorSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, DenseVectorGenerator generator) {
        super(name, language, dao, disambig);
        this.generator = generator;
        this.articleFeatures = generator.getFeatureMatrix();
        if (articleFeatures == null) {
            // TODO: build the article features if necessary.
            throw new IllegalArgumentException();
        }

        this.config = new SRConfig();
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        float [] vector1 = null;
        float [] vector2 = null;
        // try using phrases directly
        try {
            vector1 = generator.getVector(phrase1);
            vector2 = generator.getVector(phrase2);
        } catch (UnsupportedOperationException e) {
            // try using other methods
        }
        if (vector1 == null || vector2 == null) {
            return super.similarity(phrase1, phrase2, explanations);
        } else {
            SRResult result= new SRResult(SimUtils.cosineSimilarity(vector1, vector2));
            if(explanations) {
                result.setExplanations(generator.getExplanations(phrase1, phrase2, vector1, vector2, result));
            }
            return normalize(result);
        }
    }


    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        try {
            float [] v1 = getPageVector(pageId1);
            float [] v2 = getPageVector(pageId2);
            SRResult result = new SRResult(normalize(SimUtils.cosineSimilarity(v1, v2)));
            if (explanations) {
               result.setExplanations(generator.getExplanations(pageId1, pageId2, v1, v2, result));
            }
            return result;
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        try {
            // try using phrases directly
            float [] vector = generator.getVector(phrase);
            return mostSimilar(vector, maxResults, validIds);
        } catch (UnsupportedOperationException e) {
            // try using other methods
            return super.mostSimilar(phrase, maxResults, validIds);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        try {
            return mostSimilar(getPageVector(pageId), maxResults, validIds);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    public SRResultList mostSimilar(final float [] vector, int maxResults, TIntSet validIds) throws IOException {
        if (vector == null) {
            return new SRResultList(0);
        }
        SRResultList result;
        if (accelerator != null) {
            int n = (int) Math.max(minAcceleratorCandidates, maxResults * acceleratorMultiplier);
            Neighborhood nhood = accelerator.query(vector, maxResults, n, validIds);
            result = new SRResultList(nhood.size());
            for (int i = 0; i < nhood.size(); i++) {
                result.set(i, nhood.getId(i), nhood.getScore(i));
            }
        } else {
            final Leaderboard board = new Leaderboard(maxResults);
            if (validIds == null) {
                for (DenseMatrixRow row : articleFeatures) {
                    board.tallyScore(row.getRowIndex(), SimUtils.cosineSimilarity(row.getValues(), vector));
                }
            } else {
                validIds.forEach(new TIntProcedure() {
                    @Override
                    public boolean execute(int id) {
                        try {
                            float [] v = getPageVector(id);
                            if (v != null) {
                                board.tallyScore(id, SimUtils.cosineSimilarity(v, vector));
                            }
                        } catch (Exception e) {
                            LOG.warn("similarity for " + id + " failed: ", e);
                        }
                        return true;
                    }
                });
            }
            result = board.getTop();
        }
        return normalize(result);
    }

    /**
     * Train the similarity() function.
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset
     */
    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        super.trainSimilarity(dataset);     // DO nothing, for now.
    }

    /**
     * @see org.wikibrain.sr.SRMetric#trainMostSimilar(org.wikibrain.sr.dataset.Dataset, int, gnu.trove.set.TIntSet)
     */
    @Override
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
        try {
            RandomProjectionKNNFinder knn = new RandomProjectionKNNFinder(articleFeatures);
            knn.build();
            accelerator = knn;
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected exception: " + e);
        }
        super.trainMostSimilar(dataset, numResults, validIds);
    }

    @Override
    public double[][] cosimilarity(int pageIds[]) throws DaoException {
        return cosimilarity(pageIds, pageIds);
    }

    @Override
    public double[][] cosimilarity(String phrases[]) throws DaoException {
        return cosimilarity(phrases, phrases);
    }

    /**
     * Calculates the cosimilarity matrix between phrases.
     * First tries to use generator to get phrase vectors directly, but some generators will not support this.
     * Falls back on disambiguating phrase vectors to page ids.
     *
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    @Override
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[]) throws DaoException {
        if (rowPhrases.length == 0 || colPhrases.length == 0) {
            return new double[rowPhrases.length][colPhrases.length];
        }
        float [][] rowVectors = new float[rowPhrases.length][];
        float [][] colVectors = new float[colPhrases.length][];
        try {
            for (int i = 0; i < rowPhrases.length; i++) {
                rowVectors[i] = generator.getVector(rowPhrases[i]);
            }
            for (int i = 0; i < colPhrases.length; i++) {
                colVectors[i] = generator.getVector(colPhrases[i]);
            }
        } catch (UnsupportedOperationException e) {
            return super.cosimilarity(rowPhrases, colPhrases);
        }
        double [][] result = new double[rowVectors.length][colVectors.length];
        for (int i = 0; i < rowVectors.length; i++) {
            for (int j = 0; j < colVectors.length; j++) {
                result[i][j] = normalize(SimUtils.cosineSimilarity(rowVectors[i], colVectors[j]));
            }
        }
        return result;
    }

    /**
     * Computes the cosimilarity matrix between pages.
     * @param rowIds
     * @param colIds
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    @Override
    public double[][] cosimilarity(int rowIds[], int colIds[]) throws DaoException {
        try {
            if (rowIds.length == 0 || colIds.length == 0) {
                return new double[rowIds.length][colIds.length];
            }
            float[][] rowVectors = new float[rowIds.length][];
            float[][] colVectors = new float[colIds.length][];
            for (int i = 0; i < rowIds.length; i++) {
                rowVectors[i] = getPageVector(rowIds[i]);
            }
            for (int i = 0; i < colIds.length; i++) {
                colVectors[i] = getPageVector(colIds[i]);
            }
            double[][] result = new double[rowVectors.length][colVectors.length];
            for (int i = 0; i < rowVectors.length; i++) {
                for (int j = 0; j < colVectors.length; j++) {
                    result[i][j] = normalize(SimUtils.cosineSimilarity(rowVectors[i], colVectors[j]));
                }
            }
            return result;
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public void read() throws IOException {
        super.read();
        RandomProjectionKNNFinder knn = new RandomProjectionKNNFinder(articleFeatures);
        if (knn.load(new File(getDataDir(), "knn.bin"))) {
            accelerator = knn;
        }
    }

    @Override
    public void write() throws IOException {
        super.write();
        if (accelerator != null) accelerator.save(new File(getDataDir(), "knn.bin"));
    }

    /**
     * Returns the vector associated with a page, or null.
     * @param pageId
     * @return
     */
    public float[] getPageVector(int pageId) throws IOException {
        if (articleFeatures == null) {
            try {
                return generator.getVector(pageId);
            } catch (DaoException e) {
                throw new IOException(e);
            }
        } else {
            DenseMatrixRow row = articleFeatures.getRow(pageId);
            return row == null ? null : row.getValues();
        }
    }

    public DenseVectorGenerator getGenerator() {
        return generator;
    }

    public void setAccelerator(KNNFinder accelerator) {
        this.accelerator = accelerator;
    }

    public void setAcceleratorMultiplier(double acceleratorMultiplier) {
        this.acceleratorMultiplier = acceleratorMultiplier;
    }

    public void setMinAcceleratorCandidates(int minAcceleratorCandidates) {
        this.minAcceleratorCandidates = minAcceleratorCandidates;
    }

    @Override
    public SRConfig getConfig() {
        return config;
    }

    public static class Provider extends org.wikibrain.conf.Provider<SRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("densevector")) {
                return null;
            }

            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("Monolingual requires 'language' runtime parameter.");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            Map<String, String> params = new HashMap<String, String>();
            params.put("language", language.getLangCode());
            DenseVectorGenerator generator = getConfigurator().construct(
                    DenseVectorGenerator.class, null, config.getConfig("generator"), params);
            DenseVectorSRMetric sr = new DenseVectorSRMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator"),"language", language.getLangCode()),
                    generator
            );
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
