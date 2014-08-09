package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.matrix.*;
import org.wikibrain.sr.*;
import org.wikibrain.sr.BaseSRMetric;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.utils.*;

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
 * @see org.wikibrain.sr.vector.VectorGenerator
 * @see org.wikibrain.sr.vector.VectorSimilarity
 */
public class VectorBasedMonoSRMetric extends BaseSRMetric {
    private static enum PhraseMode {
        GENERATOR,  // try to get phrase vectors from the generator directly
        CREATOR,    // try to get phrase vectors form the phrase vector creator
        BOTH,       // first try the generator, then the creator
        NONE        // don't resolve phrases at all.
    }

    private static final Logger LOG = Logger.getLogger(VectorBasedMonoSRMetric.class.getName());
    private final VectorGenerator generator;
    private final VectorSimilarity similarity;
    private final SRConfig config;
    private final PhraseVectorCreator phraseVectorCreator;

    private SparseMatrix featureMatrix;
    private SparseMatrix transposeMatrix;

    private PhraseMode phraseMode = PhraseMode.BOTH;

    public VectorBasedMonoSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, VectorGenerator generator, VectorSimilarity similarity, PhraseVectorCreator creator) {
        super(name, language, dao, disambig);
        this.generator = generator;
        this.similarity = similarity;

        this.config = new SRConfig();
        this.config.minScore = (float) similarity.getMinValue();
        this.config.maxScore = (float) similarity.getMaxValue();

        this.phraseVectorCreator = creator;
        if (creator != null) {
            creator.setMetric(this);
        }
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        if (phraseMode == PhraseMode.NONE) {
            return super.similarity(phrase1, phrase2, explanations);
        }
        TIntFloatMap vector1 = null;
        TIntFloatMap vector2 = null;
        // try using phrases directly
        if (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.GENERATOR) {
            try {
                vector1 = generator.getVector(phrase1);
                vector2 = generator.getVector(phrase2);
            } catch (UnsupportedOperationException e) {
                // try using other methods
            }
        }
        if ((vector1 == null || vector2 == null)
        &&  (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.CREATOR)) {
            if (phraseVectorCreator == null) {
                throw new IllegalStateException("phraseMode is " + phraseMode + " but phraseVectorCreator is null");
            }
            TIntFloatMap vectors[] = phraseVectorCreator.getPhraseVectors(phrase1, phrase2);
            if (vectors != null) {
                vector1 = vectors[0];
                vector2 = vectors[1];
            }
        }
        if (vector1 == null || vector2 == null) {
            // fallback on parent's phrase resolution algorithm
            return super.similarity(phrase1, phrase2, explanations);
        } else {
            SRResult result= new SRResult(similarity.similarity(vector1, vector2));
            if(explanations) {
                    result.setExplanations(generator.getExplanations(phrase1, phrase2, vector1, vector2, result));
        }
            return normalize(result);
    }
    }


    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        try {
            if (hasFeatureMatrix()) {
                // Optimization that matters: Avoid building page vectors if possible.
                SparseMatrixRow row1 = featureMatrix.getRow(pageId1);
                SparseMatrixRow row2 = featureMatrix.getRow(pageId2);
                if (row1 == null || row2 == null) {
                    return null;
                } else {
                    SRResult result= new SRResult(similarity.similarity(row1, row2));
                    if(explanations) {
                        TIntFloatHashMap tfm1=row1.asTroveMap();
                        TIntFloatHashMap tfm2=row2.asTroveMap();
                        result.setExplanations(generator.getExplanations(pageId1, pageId2, tfm1, tfm2, result));
                    }
                    return normalize(result);
                }
            } else {
                TIntFloatMap vector1 = getPageVector(pageId1);
                TIntFloatMap vector2 = getPageVector(pageId2);
                if (vector1 == null || vector2 == null) {
                    return null;
                }
                return normalize(new SRResult(similarity.similarity(vector1, vector2)));
            }
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        if (phraseMode == PhraseMode.NONE) {
            return super.mostSimilar(phrase, maxResults, validIds);
        }
        TIntFloatMap vector = null;
        // try using phrases directly
        if (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.GENERATOR) {
            try {
                vector = generator.getVector(phrase);
            } catch (UnsupportedOperationException e) {
                // try using other methods
            }
        }
        if (vector == null &&  (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.CREATOR)) {
            if (phraseVectorCreator == null) {
                throw new IllegalStateException("phraseMode is " + phraseMode + " but phraseVectorCreator is null");
            }
            vector = phraseVectorCreator.getPhraseVector(phrase);
        }
        if (vector == null) {
            // fall back on parent's phrase resolution algorithm
            return super.mostSimilar(phrase, maxResults, validIds);
        } else {
            try {
                return similarity.mostSimilar(vector, maxResults, validIds);
            } catch (IOException e) {
                throw new DaoException(e);
            }
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
     * @see org.wikibrain.sr.SRMetric#trainMostSimilar(org.wikibrain.sr.dataset.Dataset, int, gnu.trove.set.TIntSet)
     */
    @Override
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
        try {
            buildFeatureAndTransposeMatrices(validIds);
            super.trainMostSimilar(dataset, numResults, validIds);
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

    /**
     * Calculates the cosimilarity matrix between phrases.
     * First tries to use generator to get phrase vectors directly, but some generators will not support this.
     * Falls back on disambiguating phrase vectors to page ids.
     *
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws DaoException
     */
    @Override
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[]) throws DaoException {
        if (rowPhrases.length == 0 || colPhrases.length == 0) {
            return new double[rowPhrases.length][colPhrases.length];
        }
        List<TIntFloatMap> rowVectors = new ArrayList<TIntFloatMap>();
        List<TIntFloatMap> colVectors = new ArrayList<TIntFloatMap>();
        try {
            // Try to use strings directly, but generator may not support them, so fall back on disambiguation
            Map<String, TIntFloatMap> vectors = new HashMap<String, TIntFloatMap>();
            for (String s : ArrayUtils.addAll(rowPhrases, colPhrases)) {
                if (!vectors.containsKey(s)) {
                    vectors.put(s, generator.getVector(s));
                }
            }
            for (String s : rowPhrases) {
                rowVectors.add(vectors.get(s));
            }
            for (String s : colPhrases) {
                colVectors.add(vectors.get(s));
            }
        } catch (UnsupportedOperationException e) {
        }

        // If direct phrase vectors failed, try to disambiguate
        if (rowVectors.isEmpty() || colVectors.isEmpty()) {
            List<String> unique = new ArrayList<String>();
            for (String s : ArrayUtils.addAll(rowPhrases, colPhrases)) {
                if (!unique.contains(s)) {
                    unique.add(s);
                }
            }
            TIntFloatMap[] vectors = phraseVectorCreator.getPhraseVectors(unique.toArray(new String[0]));
            for (String s : rowPhrases) {
                int i = unique.indexOf(s);
                if (i < 0) throw new IllegalStateException();
                rowVectors.add(vectors[i]);
            }
            for (String s : colPhrases) {
                int i = unique.indexOf(s);
                if (i < 0) throw new IllegalStateException();
                colVectors.add(vectors[i]);
            }
        }
        return cosimilarity(rowVectors, colVectors);
    }

    /**
     * Computes the cosimilarity matrix between pages.
     * @param rowIds
     * @param colIds
     * @return
     * @throws DaoException
     */
    @Override
    public double[][] cosimilarity(int rowIds[], int colIds[]) throws DaoException {
        if (hasFeatureMatrix()) {
            // special optimized case
            TIntObjectMap<SparseMatrixRow> vectors = new TIntObjectHashMap<SparseMatrixRow>(rowIds.length + colIds.length);
            for (int id : ArrayUtils.addAll(rowIds, colIds)) {
                if (vectors.containsKey(id)) {
                    continue;
                }
                SparseMatrixRow row = null;
                try {
                    row = featureMatrix.getRow(id);
                } catch (IOException e) {
                    throw new DaoException(e);
                }
                if (row != null) {
                    vectors.put(id, row);
                }
            }
            double results[][] = new double[rowIds.length][colIds.length];
            for (int i = 0; i < rowIds.length; i++) {
                SparseMatrixRow row1 = vectors.get(rowIds[i]);
                if (row1 != null) {
                    for (int j = 0; j < colIds.length; j++) {
                        SparseMatrixRow row2 = vectors.get(colIds[j]);
                        if (row2 != null) {
                            results[i][j] = normalize(similarity.similarity(row1, row2));
                        }
                    }
                }
            }
            return results;
        } else {
            // Build up vectors for unique pages
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
    }

    /**
     * Computes the cosimilarity between a set of vectors.
     * @param rowVectors
     * @param colVectors
     * @return
     */
    protected double[][] cosimilarity(List<TIntFloatMap> rowVectors, List<TIntFloatMap> colVectors) {
        double results[][] = new double[rowVectors.size()][colVectors.size()];
        for (int i = 0; i < rowVectors.size(); i++) {
            for (int j = 0; j < colVectors.size(); j++) {
                TIntFloatMap vi = rowVectors.get(i);
                TIntFloatMap vj = colVectors.get(j);
                results[i][j] = normalize(similarity.similarity(vi, vj));
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
                WbArrayUtils.toList(validIds.toArray()),
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

        similarity.setMatrices(featureMatrix, transposeMatrix, getDataDir());
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
        if (getFeatureMatrixPath().isFile() && getTransposeMatrixPath().isFile()) {
            IOUtils.closeQuietly(featureMatrix);
            IOUtils.closeQuietly(transposeMatrix);
            featureMatrix = new SparseMatrix(getFeatureMatrixPath());
            transposeMatrix = new SparseMatrix(getTransposeMatrixPath());
            similarity.setMatrices(featureMatrix, transposeMatrix, getDataDir());
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

    /**
     * Returns the vector associated with a particular phrase
     * @param phrase
     * @return
     * @throws DaoException
     */
    public TIntFloatMap getPhraseVector(String phrase) throws DaoException {
        try {
            return generator.getVector(phrase);
        } catch (UnsupportedOperationException e) {
            return phraseVectorCreator.getPhraseVector(phrase);
        }
    }

    protected boolean hasFeatureMatrix() {
        return featureMatrix != null && featureMatrix.getNumRows() > 0;
    }

    protected boolean hasTransposeMatrix() {
        return transposeMatrix != null && transposeMatrix.getNumRows() > 0;
    }

    public VectorGenerator getGenerator() {
        return generator;
    }

    public VectorSimilarity getSimilarity() {
        return similarity;
    }

    @Override
    public SRConfig getConfig() {
        return config;
    }

    public void setPhraseMode(PhraseMode mode) {
        this.phraseMode = mode;
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
            if (!config.getString("type").equals("vector")) {
                return null;
            }

            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("Monolingual requires 'language' runtime parameter.");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            Map<String, String> params = new HashMap<String, String>();
            params.put("language", language.getLangCode());
            VectorGenerator generator = getConfigurator().construct(
                    VectorGenerator.class, null, config.getConfig("generator"), params);
            VectorSimilarity similarity = getConfigurator().construct(
                    VectorSimilarity.class,  null, config.getConfig("similarity"), params);
            PhraseVectorCreator phraseVectorCreator = null;
            if (config.hasPath("phrases")) {
                phraseVectorCreator = getConfigurator().construct(
                        PhraseVectorCreator.class, null, config.getConfig("phrases"), null);
            }
            VectorBasedMonoSRMetric sr = new VectorBasedMonoSRMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator"),"language", language.getLangCode()),
                    generator,
                    similarity,
                    phraseVectorCreator
            );
            if (config.hasPath("phraseMode")) {
                sr.setPhraseMode(PhraseMode.valueOf(config.getString("phraseMode").toUpperCase()));
            }
            configureBase(getConfigurator(), sr, config);
            if (phraseVectorCreator != null) phraseVectorCreator.setMetric(sr);
            return sr;
        }

    }
}
