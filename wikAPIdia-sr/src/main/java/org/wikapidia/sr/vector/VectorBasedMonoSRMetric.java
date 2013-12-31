package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An SR metric that represents phrases and pages using sparse numeric vectors.
 * SR scores are the result of some similarity metric. MilneWitten, ESA, and
 * Pairwise metrics all use this representation.
 *
 * <p>
 * The metric requires two subcomponents:
 * <ul>
 *     <li>A VectorGenerator class that generates the sparse vectors.</li>
 *     <li>A VectorSimilarity class that generates SR scores given two vectors.</li>
 * </ul>
 *
 * <p>
 *
 * This class also manages a feature matrix and transpose. The matrix is required
 * for calls to mostSimilar. It is not required for calls to similarity(), but will
 * be used to speed them up if available. The matrix is built when trainMostSimilar()
 * is called, but can also be explicitly built by calling
 * buildFeatureAndTransposeMatrices().
 *
 * @author Shilad Sen
 * @see org.wikapidia.sr.vector.VectorGenerator
 * @see org.wikapidia.sr.vector.VectorSimilarity
 */
public class VectorBasedMonoSRMetric extends BaseMonolingualSRMetric {
    private static final Logger LOG = Logger.getLogger(VectorBasedMonoSRMetric.class.getName());
    private final VectorGenerator generator;
    private final VectorSimilarity similarity;
    private final MetricConfig config;

    private SparseMatrix featureMatrix;
    private SparseMatrix transposeMatrix;

    private boolean resolvePhrases = false;
    private boolean trainingChangesVectors = false;

    public VectorBasedMonoSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, VectorGenerator generator, VectorSimilarity similarity) {
        super(name, language, dao, disambig);
        this.generator = generator;
        this.similarity = similarity;
        this.config = new MetricConfig();
    }


    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        if (resolvePhrases) {
            return super.similarity(phrase1, phrase2, explanations);
        }
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
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        if (resolvePhrases) {
            return super.mostSimilar(phrase, maxResults, validIds);
        }
        try {
            TIntFloatMap vector = generator.getVector(phrase);
            return similarity.mostSimilar(vector, maxResults, validIds);    // Base resolved to a page id
        } catch (UnsupportedOperationException e) {
            return super.mostSimilar(phrase, maxResults, validIds);
        } catch (IOException e) {
            throw new DaoException(e);
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
     * @see org.wikapidia.sr.MonolingualSRMetric#trainMostSimilar(org.wikapidia.sr.dataset.Dataset, int, gnu.trove.set.TIntSet)
     */
    @Override
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

    @Override
    public double[][] cosimilarity(int pageIds[]) throws DaoException {
        return cosimilarity(pageIds, pageIds);
    }

    @Override
    public double[][] cosimilarity(String phrases[]) throws DaoException {
        return cosimilarity(phrases, phrases);
    }

    @Override
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[]) throws DaoException {
        try {
            // Try to use strings directly, but generator may not support them, so fall back on disambiguation
            Map<String, TIntFloatMap> vectors = new HashMap<String, TIntFloatMap>();
            for (String s : ArrayUtils.addAll(rowPhrases, colPhrases)) {
                if (!vectors.containsKey(s)) {
                    vectors.put(s, generator.getVector(s));
                }
            }
            List<TIntFloatMap> rowVectors = new ArrayList<TIntFloatMap>();
            for (String s : rowPhrases) {
                rowVectors.add(vectors.get(s));
            }
            List<TIntFloatMap> colVectors = new ArrayList<TIntFloatMap>();
            for (String s : colPhrases) {
                colVectors.add(vectors.get(s));
            }
            return cosimilarity(rowVectors, colVectors);
        } catch (UnsupportedOperationException e) {
            // Disambiguate phrases to ids, use these to call the id version of cosimilarity
            List<LocalString> unique = new ArrayList<LocalString>();
            for (String s : ArrayUtils.addAll(rowPhrases, colPhrases)) {
                LocalString ls = new LocalString(getLanguage(), s);
                if (!unique.contains(ls)) {
                    unique.add(ls);
                }
            }
            List<LocalId> pageIds = getDisambiguator().disambiguateTop(unique, null);
            TIntList rowIds = new TIntArrayList();
            for (String s : rowPhrases) {
                int i = unique.indexOf(new LocalString(getLanguage(), s));
                if (i < 0) { throw new IllegalStateException(); }
                rowIds.add(pageIds.get(i) == null ? -1 : pageIds.get(i).getId());
            }
            TIntList colIds = new TIntArrayList();
            for (String s : colPhrases) {
                int i = unique.indexOf(new LocalString(getLanguage(), s));
                if (i < 0) { throw new IllegalStateException(); }
                colIds.add(pageIds.get(i) == null ? -1 : pageIds.get(i).getId());
            }
            return cosimilarity(rowIds.toArray(), colIds.toArray());
        }
    }

    @Override
    public double[][] cosimilarity(int rowIds[], int colIds[]) throws DaoException {
        // Build all vectors
        Map<Integer, TIntFloatMap> vectors = new HashMap<Integer, TIntFloatMap>();
        for (int pageId : ArrayUtils.addAll(colIds, rowIds)) {
            if (!vectors.containsKey(pageId)) {
                try {
                    vectors.put(pageId, getPageVector(pageId));
                } catch (IOException e) {
                    throw new DaoException(e);
                }
            }
        }
        List<TIntFloatMap> rowVectors = new ArrayList<TIntFloatMap>();
        for (int rowId : rowIds) {
            rowVectors.add(vectors.get(rowId));
        }
        List<TIntFloatMap> colVectors = new ArrayList<TIntFloatMap>();
        for (int colId : colIds) {
            colVectors.add(vectors.get(colId));
        }
        return cosimilarity(rowVectors, colVectors);
    }

    protected double[][] cosimilarity(List<TIntFloatMap> rowVectors, List<TIntFloatMap> colVectors) {
        double results[][] = new double[rowVectors.size()][colVectors.size()];
        for (int i = 0; i < rowVectors.size(); i++) {
            for (int j = 0; j < colVectors.size(); j++) {
                TIntFloatMap vi = rowVectors.get(i);
                TIntFloatMap vj = colVectors.get(j);
                results[i][j] = similarity.similarity(vi, vj);
            }
        }
        return results;
    }

    /**
     * Rebuild the feature and transpose matrices.
     * If the matrices are available from the feature generator, they will be used.
     * If not, they will be regenerated.
     * @param validIds
     * @throws IOException
     */
    public synchronized void buildFeatureAndTransposeMatrices(TIntSet validIds) throws IOException {
        if (validIds == null) {
            validIds = getAllPageIds();
        }

        IOUtils.closeQuietly(featureMatrix);
        IOUtils.closeQuietly(transposeMatrix);

        featureMatrix = null;
        transposeMatrix = null;

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

        getDataDir().mkdirs();
        new SparseMatrixTransposer(featureMatrix, getTransposeMatrixPath())
                .transpose();
        transposeMatrix = new SparseMatrix(getTransposeMatrixPath());

        similarity.setMatrices(featureMatrix, transposeMatrix);
    }

    private TIntSet getAllPageIds() throws IOException {
        TIntSet validIds;DaoFilter filter = new DaoFilter()
                .setLanguages(getLanguage())
                .setDisambig(false)
                .setRedirect(false)
                .setNameSpaces(NameSpace.ARTICLE);
        validIds = new TIntHashSet();
        try {
            for (LocalPage page : (Iterable<LocalPage>)getLocalPageDao().get(filter)) {
                validIds.add(page.getLocalId());
            }
        } catch (DaoException e) {
            throw new IOException(e);
        }
        return validIds;
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

    public void setResolvePhrases(boolean resolve) {
        this.resolvePhrases = resolve;
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
            Map<String, String> params = new HashMap<String, String>();
            params.put("language", language.getLangCode());
            VectorGenerator generator = getConfigurator().construct(
                    VectorGenerator.class, null, config.getConfig("generator"), params);
            VectorSimilarity similarity = getConfigurator().construct(
                    VectorSimilarity.class,  null, config.getConfig("similarity"), params);
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
            if (config.hasPath("resolvePhrases")) {
                sr.setResolvePhrases(config.getBoolean("resolvePhrases"));
            }
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
