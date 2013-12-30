package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.io.IOUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.*;
import org.wikapidia.sr.BaseMonolingualSRMetric;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class VectorBasedMonoSRMetric extends BaseMonolingualSRMetric {
    private static final Logger LOG = Logger.getLogger(VectorBasedMonoSRMetric.class.getName());
    private final VectorGenerator generator;
    private final VectorSimilarity similarity;
    private final MetricConfig config;

    private SparseMatrix featureMatrix;
    private SparseMatrix transposeMatrix;

    private boolean trainingChangesVectors = false;

    public VectorBasedMonoSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, VectorGenerator generator, VectorSimilarity similarity) {
        super(name, language, dao, disambig);
        this.generator = generator;
        this.similarity = similarity;
        this.config = new MetricConfig();
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        TIntFloatMap vector1 = null;
        TIntFloatMap vector2 = null;
        try {
            vector1 = generator.getVector(phrase1);
            vector2 = generator.getVector(phrase2);
        } catch (UnsupportedOperationException e) {
            return super.similarity(phrase1, phrase2, explanations);    // disambiguates to ids
        }
        SRResult result = new SRResult(similarity.similarity(vector1, vector2));
        if (explanations) {
            similarity.addExplanations(vector1, vector2, result);
        }
        return normalize(result);
    }

    /**
     * Train the similarity() function.
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset
     */
    public void trainSimilarity(Dataset dataset) throws DaoException {
        super.trainSimilarity(dataset);     // DO nothing, for now.
    }

    /**
     * @see org.wikapidia.sr.MonolingualSRMetric#trainMostSimilar(org.wikapidia.sr.dataset.Dataset, int, gnu.trove.set.TIntSet)
     */
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
        try {
            if (trainingChangesVectors) {
                super.trainMostSimilar(dataset, numResults, validIds);
                buildFeatureAndTransposeMatrices(validIds);
            } else {
                buildFeatureAndTransposeMatrices(validIds);
                super.trainMostSimilar(dataset, numResults, validIds);
        }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "training failed", e);
            throw new RuntimeException(e);  // somewhat unexpected...
        }
    }

    /**
     * Rebuild the feature and transpose matrices.
     * If the matrices are available from the feature generator, they will be used.
     * If not, they will be regenerated.
     * @param validIds
     * @throws IOException
     */
    public synchronized void buildFeatureAndTransposeMatrices(TIntSet validIds) throws IOException {
        IOUtils.closeQuietly(featureMatrix);
        IOUtils.closeQuietly(transposeMatrix);

        featureMatrix = null;
        transposeMatrix = null;
        try {
            featureMatrix = generator.getFeatureMatrix();
            transposeMatrix = generator.getFeatureTransposeMatrix();
        } catch (UnsupportedOperationException e) {
        }
        if (featureMatrix == null) {
            getDataDir().mkdirs();
            ValueConf vconf = new ValueConf((float)similarity.getMinValue(),
                                            (float)similarity.getMaxValue());
            final SparseMatrixWriter writer = new SparseMatrixWriter(getFeatureMatrixPath(), vconf);
            ParallelForEach.loop(
                    WpArrayUtils.toList(validIds.toArray()),
                    WpThreadUtils.getMaxThreads(),
                    new Procedure<Integer>() {
                        public void call(Integer pageId) throws IOException {
                            TIntFloatMap scores = getPageVector(pageId);
                            if (scores != null && !scores.isEmpty()) {
                                writer.writeRow(new SparseMatrixRow(writer.getValueConf(), pageId, scores));
                            }
                        }
                    }, 10000);
            writer.finish();

            // Reload the feature matrix
            featureMatrix = new SparseMatrix(getFeatureMatrixPath());
        }
        if (transposeMatrix == null) {
            getDataDir().mkdirs();
            new SparseMatrixTransposer(featureMatrix, getTransposeMatrixPath())
                    .transpose();
            transposeMatrix = new SparseMatrix(getTransposeMatrixPath());
        }
        similarity.setMatrices(featureMatrix, transposeMatrix);
    }

    protected File getFeatureMatrixPath() {
        return new File(getDataDir(), "feature.matrix");
    }

    protected File getTransposeMatrixPath() {
        return new File(getDataDir(), "featureTranspose.matrix");
    }

    @Override
    public void read() throws IOException {
        super.read();
        if (getFeatureMatrixPath().isFile()) {
            featureMatrix = new SparseMatrix(getFeatureMatrixPath());
        }
        if (getTransposeMatrixPath().isFile()) {
            featureMatrix = new SparseMatrix(getTransposeMatrixPath());
        }
    }

    /**
     * Returns the vector associated with a page, or null.
     * @param pageId
     * @return
     */
    public TIntFloatMap getPageVector(int pageId) throws IOException {
        if (hasFeatureMatrix()) {
            SparseMatrixRow row = featureMatrix.getRow(pageId);
            return row == null ? null : row.asTroveMap();
        } else {
            try {
                return generator.getVector(pageId);
            } catch (DaoException e) {
                throw new IOException(e);
            }
        }
    }

    protected boolean hasFeatureMatrix() {
        return featureMatrix != null && featureMatrix.getNumRows() > 0;
    }

    protected boolean hasTransposeMatrix() {
        return transposeMatrix != null && transposeMatrix.getNumRows() > 0;
    }


    @Override
    public MetricConfig getMetricConfig() {
        return config;
    }

    public void setTrainingChangesVectors(boolean trainingChangesVectors) {
        this.trainingChangesVectors = trainingChangesVectors;
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        TIntFloatMap vector1 = null;
        TIntFloatMap vector2 = null;
        try {
            vector1 = getPageVector(pageId1);
            vector2 = getPageVector(pageId2);
        } catch (IOException e) {
            throw new DaoException(e);
        }
        if (vector1 == null || vector2 == null) {
            return null;
        }
        SRResult result = new SRResult(similarity.similarity(vector1, vector2));
        if (explanations) {
            similarity.addExplanations(vector1, vector2, result);
        }
        return normalize(result);
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException, IOException {
        try {
            TIntFloatMap vector = generator.getVector(phrase);
            return similarity.mostSimilar(vector, maxResults, validIds);    // Base resolved to a page id
        } catch (UnsupportedOperationException e) {
            return super.mostSimilar(phrase, maxResults, validIds);
        }
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        try {
            TIntFloatMap vector = getPageVector(pageId);
            if (vector == null) return null;
            return similarity.mostSimilar(vector, maxResults, validIds);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public TIntDoubleMap getVector(int id) throws DaoException {
        throw new UnsupportedOperationException();  // TODO: remove me
    }
    public static class Provider extends org.wikapidia.conf.Provider<MonolingualSRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return MonolingualSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public MonolingualSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("vector")) {
                return null;
            }

            if (!runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("Monolingual requires 'language' runtime parameter.");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            VectorGenerator generator = getConfigurator().construct(
                    VectorGenerator.class, null, config.getConfig("generator"), null);
            VectorSimilarity similarity = getConfigurator().construct(
                    VectorSimilarity.class,  null, config.getConfig("similarity"), null);
            VectorBasedMonoSRMetric sr = new VectorBasedMonoSRMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    generator,
                    similarity
            );
            if (config.hasPath("trainingChangesVectors")) {
                sr.setTrainingChangesVectors(config.getBoolean("trainingChangesVectors"));
            }
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
