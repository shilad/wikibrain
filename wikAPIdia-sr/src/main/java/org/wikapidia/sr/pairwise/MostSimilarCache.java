package org.wikapidia.sr.pairwise;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.*;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.normalize.IdentityNormalizer;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * A convenience container for the trio of matrices needed for SR metrics:
 * 1. The feature matrix
 * 2. The transpose of the feature matrix
 * 3. The cosimilarity matrix.
 *
 * Some metrics don't support the feature and transpose metrics.
 * In this case, they'll only have the cosimilarity matrix.
 *
 * @author Shilad Sen
 */
public class MostSimilarCache implements Closeable {
    private static final Logger LOG = Logger.getLogger(MostSimilarCache.class.getName());

    public static final String COSIMILARITY_MATRIX = "cosimilarityMatrix";
    public static final String FEATURE_TRANSPOSE_MATRIX = "featureTransposeMatrix";
    public static final String FEATURE_MATRIX = "featureMatrix";
    private final PairwiseSimilarity similarity;

    private final File dir;

    private MonolingualSRMetric monoSr;
    private UniversalSRMetric universalSr;

    private SparseMatrix featureMatrix = null;
    private SparseMatrix featureTransposeMatrix = null;
    private SparseMatrix cosimilarityMatrix = null;

    /**
     * @see #MostSimilarCache(org.wikapidia.sr.MonolingualSRMetric, PairwiseSimilarity, java.io.File)
     */
    public MostSimilarCache(MonolingualSRMetric metric, File dir) {
        this(metric, null, dir);
    }

    /**
     * Creates a new most similar cache.
     * If similarity is null, feature matrix and transpose will not be used.
     *
     * @param metric
     * @param similarity
     * @param dir
     */
    public MostSimilarCache(MonolingualSRMetric metric, PairwiseSimilarity similarity, File dir) {
        this.monoSr = metric;
        this.similarity = similarity;
        this.dir = dir;
        if (!this.dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
            dir.mkdirs();
        }
    }

    public MostSimilarCache(UniversalSRMetric metric, PairwiseSimilarity similarity, File dir) {
        throw new UnsupportedOperationException();
    }

    public boolean hasCachedMostSimilarVectors() {
        return (featureMatrix != null &&
                featureMatrix.getNumRows() > 0 &&
                featureTransposeMatrix != null &&
                featureTransposeMatrix.getNumRows() > 0);
    }

    /**
     * Closes existing matrices.
     */
    public void clear() {
        close();
        FileUtils.deleteQuietly(getFeatureMatrixPath());
        FileUtils.deleteQuietly(getFeatureTransposeMatrixPath());
        FileUtils.deleteQuietly(getCosimilarityMatrixPath());
    }

    /**
     * Reads in available matrices.
     * Throws an IOException if matrices are invalid.
     */
    public void read() throws IOException {
        if (hasAllReadableMatrices()) {
            featureMatrix = readMatrix(FEATURE_MATRIX);
            featureTransposeMatrix = readMatrix(FEATURE_TRANSPOSE_MATRIX);
            cosimilarityMatrix = readMatrix(COSIMILARITY_MATRIX);
        } else if (hasJustReadableCosimilarity()) {
            featureMatrix = null;
            featureTransposeMatrix = null;
            cosimilarityMatrix = readMatrix(COSIMILARITY_MATRIX);
        } else if (hasJustReadableFeatureAndTranspose()) {
            featureMatrix = readMatrix(FEATURE_MATRIX);
            featureTransposeMatrix = readMatrix(FEATURE_TRANSPOSE_MATRIX);
            cosimilarityMatrix = null;
        } else {
            throw new IOException("No readable matrices");
        }
    }

    /**
     * @return True if it seems like matrixes exist and read() can be safely called.
     * If any of the matrices are corrupt, read may still fail.
     */
    public boolean hasReadableMatrices() {
        return hasAllReadableMatrices() || hasJustReadableCosimilarity() || hasJustReadableFeatureAndTranspose();
    }

    protected File getChildFile(String name) {
        return new File(dir, name);
    }

    protected boolean hasChildFile(String name) {
        return getChildFile(name).isFile();
    }

    protected boolean hasAllReadableMatrices() {
        return  (hasChildFile(FEATURE_MATRIX) && hasChildFile(FEATURE_TRANSPOSE_MATRIX) && hasChildFile(COSIMILARITY_MATRIX));
    }

    protected boolean hasJustReadableFeatureAndTranspose() {
        return (hasChildFile(FEATURE_MATRIX) && hasChildFile(FEATURE_TRANSPOSE_MATRIX) && !hasChildFile(COSIMILARITY_MATRIX));
    }

    protected boolean hasJustReadableCosimilarity() {
        return (!hasChildFile(FEATURE_MATRIX) && !hasChildFile(FEATURE_TRANSPOSE_MATRIX) && hasChildFile(COSIMILARITY_MATRIX));
    }

    protected File getFeatureMatrixPath() {
        return getChildFile(FEATURE_MATRIX);
    }

    protected File getFeatureTransposeMatrixPath() {
        return getChildFile(FEATURE_TRANSPOSE_MATRIX);
    }

    protected File getCosimilarityMatrixPath() {
        return getChildFile(COSIMILARITY_MATRIX);
    }

    public SparseMatrix getFeatureMatrix() {
        return featureMatrix;
    }

    public SparseMatrix getFeatureTransposeMatrix() {
        return featureTransposeMatrix;
    }

    public SparseMatrix getCosimilarityMatrix() {
        return cosimilarityMatrix;
    }

    /**
     * Returns the SRResult list associated with the requested page and constraints, or null
     * if the cache cannot answer the request.
     *
     * @param wpId Id whose most similar result is being queried.
     * @param numResults The number of requested results.
     * @param validIds Ids that may be included in the result list.
     * @return
     * @throws IOException
     * @throws DaoException
     */
    public SRResultList mostSimilar(int wpId, int numResults, TIntSet validIds) throws IOException, DaoException {
        long l = System.currentTimeMillis();
        try {
            // First see if we have the results directly cached
            if (cosimilarityMatrix != null) {
                MatrixRow row = cosimilarityMatrix.getRow(wpId);
                if (row != null && row.getNumCols() >= numResults ) {
                    SRResultList results = rowToResultList(row, numResults, validIds);
                    if (results != null && results.numDocs() >= numResults) {
                        return normalize(results);
                    }
                }
            }

            // Next try to recompute them from the feature and transpose matrices
            if (similarity != null && featureMatrix != null && featureTransposeMatrix != null) {
                return normalize(similarity.mostSimilar(this, wpId, numResults, validIds));
            }

            // We cannot complete the request
            return null;
        } finally {
//            System.err.println("ellapsed millis is " + (System.currentTimeMillis() - l));
        }
    }

    /**
     * Returns the SRResult list associated with the requested page and constraints, or null
     * if the cache cannot answer the request.
     *
     * @param vector The query vector.
     * @param numResults The number of requested results.
     * @param validIds Ids that may be included in the result list.
     * @return
     * @throws IOException
     * @throws DaoException
     */
    public SRResultList mostSimilar(TIntFloatMap vector, int numResults, TIntSet validIds) throws IOException, DaoException {
        if (similarity != null && featureMatrix != null && featureTransposeMatrix != null) {
            return normalize(similarity.mostSimilar(this, vector, numResults, validIds));
        } else {
            return null;
        }
    }

    /**
     * Closes any open cache matrices.
     */
    public void close() {
        IOUtils.closeQuietly(featureMatrix);
        IOUtils.closeQuietly(featureTransposeMatrix);
        IOUtils.closeQuietly(cosimilarityMatrix);
        featureMatrix = null;
        featureTransposeMatrix = null;
        cosimilarityMatrix = null;
    }

    /**
     * Writes the feature and transpose matrices. And loads them into memory.
     * @param rowIds The article ids whose features should be calculated.
     * @param maxThreads Maximum number of threads to use
     */
    public void writeFeatureAndTransposeMatrix(final int rowIds[], final int maxThreads) throws WikapidiaException, InterruptedException, IOException {
        ensureDataDirectoryExists();

        // Write the feature matrix
        ValueConf vconf = new ValueConf();
        final SparseMatrixWriter writer = new SparseMatrixWriter(getFeatureMatrixPath(), vconf);
        ParallelForEach.loop(intArrayToList(rowIds), maxThreads,
                new Procedure<Integer>() {
                    public void call(Integer wpId) throws IOException, DaoException, WikapidiaException {
                        writeFeatureVector(writer, wpId);
                    }
                }, 10000);
        writer.finish();

        // Write the transpose
        SparseMatrixTransposer transposer = new SparseMatrixTransposer(
                new SparseMatrix(getFeatureMatrixPath()),
                getFeatureTransposeMatrixPath());
        transposer.transpose();

        // Reload existing matrices
        IOUtils.closeQuietly(featureMatrix);
        IOUtils.closeQuietly(featureTransposeMatrix);
        featureMatrix = readMatrix(FEATURE_MATRIX);
        featureTransposeMatrix = readMatrix(FEATURE_TRANSPOSE_MATRIX);
    }

    /**
     * Writes the feature and transpose matrices. And loads them into memory.
     * @param rowIds The article ids whose most similar lists should be cached.
     * @param colIds The article ids that may appear in the similar result lists.
     * @param maxSimsPerDoc The maximum size of a most simlar result list.
     * @param maxThreads Maximum number of threads to use
     */
    public void writeCosimilarity(final int rowIds[], final int colIds[], final int maxSimsPerDoc, int maxThreads) throws IOException, InterruptedException {
        ensureDataDirectoryExists();

        final AtomicInteger idCounter = new AtomicInteger();
        final AtomicLong cellCounter = new AtomicLong();
        ValueConf vconf;
        if (similarity == null) {
            vconf = new ValueConf();    // TODO: fixme
        } else {
            vconf = new ValueConf((float)similarity.getMinValue(), (float)similarity.getMaxValue());
        }
        final SparseMatrixWriter writer = new SparseMatrixWriter(getCosimilarityMatrixPath(), vconf);
        final TIntSet colIdSet = colIds == null ? null : new TIntHashSet(colIds);
        Normalizer simNormalizer = monoSr.getSimilarityNormalizer();
        Normalizer mostSimNormalizer = monoSr.getMostSimilarNormalizer();
        monoSr.setMostSimilarNormalizer(new IdentityNormalizer());
        monoSr.setSimilarityNormalizer(new IdentityNormalizer());
        try {
            ParallelForEach.loop(intArrayToList(rowIds), maxThreads,
                    new Procedure<Integer>() {
                        public void call(Integer wpId) throws IOException, DaoException {
                            writeSim(writer, wpId, colIdSet, maxSimsPerDoc, idCounter, cellCounter);
                        }
                    }, Integer.MAX_VALUE);
        } finally {
            monoSr.setSimilarityNormalizer(simNormalizer);
            monoSr.setMostSimilarNormalizer(mostSimNormalizer);
        }

        LOG.info("wrote " + cellCounter.get() + " non-zero similarity cells");
        writer.finish();
        cosimilarityMatrix = readMatrix(COSIMILARITY_MATRIX);
    }

    private SparseMatrix readMatrix(String name) throws IOException {
        return new SparseMatrix(getChildFile(name));
    }

    private void writeSim(SparseMatrixWriter writer, Integer wpId, TIntSet colIds, int maxSimsPerDoc, AtomicInteger idCounter, AtomicLong cellCounter) throws IOException, DaoException {
        if (idCounter.incrementAndGet() % 10000 == 0) {
            LOG.info("finding matches for page " + idCounter.get());
        }
        SRResultList scores;
        if (similarity != null) {
            scores = similarity.mostSimilar(this, wpId, maxSimsPerDoc, colIds);
        } else {
            scores = monoSr.mostSimilar(wpId, maxSimsPerDoc, colIds);
        }
        if (scores != null) {
            int ids[] = scores.getIds();
            cellCounter.getAndIncrement();
            writer.writeRow(new SparseMatrixRow(writer.getValueConf(), wpId, ids, scores.getScoresAsFloat()));
        }
    }

    private void writeFeatureVector(SparseMatrixWriter writer, Integer id) throws WikapidiaException {
        TIntDoubleMap scores;
        try {
            if (monoSr !=null){
                scores = monoSr.getVector(id);
            } else if (universalSr!=null){
                scores = universalSr.getVector(id);
            } else {
                throw new IllegalStateException("SRFeatureMatrixWriter does not have a local or universal metric defined.");
            }
        } catch (DaoException e){
            throw new WikapidiaException(e);
        }
        if (scores == null || scores.isEmpty()) {
            return;
        }
        LinkedHashMap<Integer,Float> linkedHashMap = new LinkedHashMap<Integer, Float>();
        for (int i : scores.keys()){
            linkedHashMap.put(i,(float)scores.get(i));
        }
        try {
            writer.writeRow(new SparseMatrixRow(writer.getValueConf(), id, linkedHashMap));
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
    }

    private SRResultList rowToResultList(MatrixRow row, int maxResults, TIntSet validIds) {
        Leaderboard leaderboard = new Leaderboard(maxResults);
        for (int i=0; i<row.getNumCols() ; i++){
            int wpId2 = row.getColIndex(i);
            if (validIds == null || validIds.contains(wpId2)){
                leaderboard.tallyScore(wpId2, row.getColValue(i));
            }
        }
        SRResultList results = leaderboard.getTop();
        results.sortDescending();
        return results;
    }

    private List<Integer> intArrayToList(int[] ints) {
        List<Integer> result = new ArrayList<Integer>();
        for (int id : ints) { result.add(id); }
        return result;
    }

    private void ensureDataDirectoryExists() {
        if (!dir.isDirectory()) { dir.mkdirs(); }
    }

    private SRResultList normalize(SRResultList list) {
        if (list == null) {
            return null;
        } else {
            return monoSr.getMostSimilarNormalizer().normalize(list);
        }
    }
}
