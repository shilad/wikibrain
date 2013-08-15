package org.wikapidia.sr.utils;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.*;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.pairwise.PairwiseSimilarity;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.*;
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
 * @author Shilad Sen
 */
public class SRMatrices implements Closeable {
    private static final Logger LOG = Logger.getLogger(SRMatrices.class.getName());

    public static final String COSIMILARITY_MATRIX = "cosimilarityMatrix";
    public static final String FEATURE_TRANSPOSE_MATRIX = "featureTransposeMatrix";
    public static final String FEATURE_MATRIX = "featureMatrix";
    private final Language language;
    private final PairwiseSimilarity similarity;

    private final File dir;

    private LocalSRMetric localSr;
    private UniversalSRMetric universalSr;

    private SparseMatrix featureMatrix = null;
    private SparseMatrix featureTransposeMatrix = null;
    private SparseMatrix cosimilarityMatrix = null;

    public SRMatrices(LocalSRMetric metric, Language language, PairwiseSimilarity similarity, File dir) {
        this.localSr = metric;
        this.language = language;
        this.similarity = similarity;
        this.dir = dir;
        if (!this.dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
            dir.mkdirs();
        }
    }

    public SRMatrices(UniversalSRMetric metric, PairwiseSimilarity similarity, File dir) {
        this.universalSr = metric;
        this.language = null;
        this.similarity = similarity;
        this.dir = dir;
        if (!this.dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
            dir.mkdirs();
        }
    }

    public void setMatrices(SparseMatrix featureMatrix, SparseMatrix featureTransposeMatrix, SparseMatrix cosimilarityMatrix) {
        this.featureMatrix = featureMatrix;
        this.featureTransposeMatrix = featureTransposeMatrix;
        this.cosimilarityMatrix = cosimilarityMatrix;
    }

    public void clear() {
        FileUtils.deleteQuietly(getChildFile(FEATURE_MATRIX));
        FileUtils.deleteQuietly(getChildFile(FEATURE_TRANSPOSE_MATRIX));
        FileUtils.deleteQuietly(getChildFile(COSIMILARITY_MATRIX));
    }

    private File getChildFile(String name) {
        return new File(dir, name);
    }

    private boolean hasChildFile(String name) {
        return getChildFile(name).isFile();
    }

    /**
     * TODO: make sure matrices are actually readable.
     * @return True if readable files exist for all three matrices.
     */
    public boolean hasReadableMatrices() {
        return getChildFile(FEATURE_MATRIX).isFile()
        &&     getChildFile(FEATURE_TRANSPOSE_MATRIX).isFile()
        &&     getChildFile(COSIMILARITY_MATRIX).isFile();
    }

    /**
     * TODO: specify parameters for num pages and page size
     */
    public void readMatrices() throws IOException {
        featureMatrix = new SparseMatrix(getChildFile(FEATURE_MATRIX));
        featureTransposeMatrix = new SparseMatrix(getChildFile(FEATURE_TRANSPOSE_MATRIX));
        cosimilarityMatrix = new SparseMatrix(getChildFile(COSIMILARITY_MATRIX));
    }

    public File getFeatureMatrixPath() {
        return getChildFile(FEATURE_MATRIX);
    }

    public File getFeatureTransposeMatrixPath() {
        return getChildFile(FEATURE_TRANSPOSE_MATRIX);
    }

    public File getCosimilarityMatrixPath() {
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

    public void close() {
        IOUtils.closeQuietly(featureMatrix);
        IOUtils.closeQuietly(featureTransposeMatrix);
        IOUtils.closeQuietly(cosimilarityMatrix);
    }

    public void writeFeatureMatrix(final int rowIds[], final int threads) throws WikapidiaException, InterruptedException, IOException {
        List<Integer> wpIds2 = new ArrayList<Integer>();
        for (int id : rowIds) { wpIds2.add(id); }
        writeFeatureMatrix(wpIds2, threads);
    }

    public void writeFeatureMatrix(List<Integer> rowIds, int threads) throws WikapidiaException, InterruptedException, IOException {
        ValueConf vconf = new ValueConf();
        final SparseMatrixWriter writer = new SparseMatrixWriter(getFeatureMatrixPath(), vconf);
        ParallelForEach.loop(rowIds, threads, new Procedure<Integer>() {
            public void call(Integer wpId) throws IOException, DaoException, WikapidiaException {
                writeFeatureVector(writer, wpId);
            }
        }, 10000);
        try {
            writer.finish();
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
    }

    public void writeTranspose() throws IOException {
        SparseMatrixTransposer transposer = new SparseMatrixTransposer(
                new SparseMatrix(getFeatureMatrixPath()),
                getFeatureTransposeMatrixPath(),
                1024*1024*500); //500mb buffer
        transposer.transpose();
    }

    public void writeCosimilarity(final int rowIds[], final int colIds[], final int maxSimsPerDoc) throws IOException, InterruptedException {
        List<Integer> wpIds2 = new ArrayList<Integer>();
        for (int id : rowIds) { wpIds2.add(id); }
        writeCosimilarity(wpIds2, colIds, maxSimsPerDoc);
    }

    public void writeCosimilarity(List<Integer> rowIds, final int colIds[], final int maxSimsPerDoc) throws IOException {
        final AtomicInteger idCounter = new AtomicInteger();
        final AtomicLong cellCounter = new AtomicLong();
        ValueConf vconf = new ValueConf((float)similarity.getMinValue(), (float)similarity.getMaxValue());
        final SparseMatrixWriter writer = new SparseMatrixWriter(getCosimilarityMatrixPath(), vconf);
        final TIntSet colIdSet = colIds == null ? null : new TIntHashSet(colIds);
        ParallelForEach.loop(rowIds, new Procedure<Integer>() {
            public void call(Integer wpId) throws IOException {
                writeSim(writer, wpId, colIdSet, maxSimsPerDoc, idCounter, cellCounter);
            }
        }, Integer.MAX_VALUE);

        LOG.info("wrote " + cellCounter.get() + " non-zero similarity cells");
        writer.finish();
    }

    private void writeSim(SparseMatrixWriter writer, Integer wpId, TIntSet colIds, int maxSimsPerDoc, AtomicInteger idCounter, AtomicLong cellCounter) throws IOException {
        if (idCounter.incrementAndGet() % 10000 == 0) {
            LOG.info("finding matches for doc " + idCounter.get());
        }
        SRResultList scores = similarity.mostSimilar(wpId, maxSimsPerDoc, colIds);
        if (scores != null) {
            int ids[] = scores.getIds();
            cellCounter.getAndIncrement();
            writer.writeRow(new SparseMatrixRow(writer.getValueConf(), wpId, ids, scores.getScoresAsFloat()));
        }
    }

    private void writeFeatureVector(SparseMatrixWriter writer, Integer id) throws WikapidiaException {
        TIntDoubleMap scores;
        try {
            if (localSr!=null){
                scores = localSr.getVector(id, language);
            } else if (universalSr!=null){
                scores = universalSr.getVector(id);
            } else {
                throw new IllegalStateException("SRFeatureMatrixWriter does not have a local or universal metric defined.");
            }
        } catch (DaoException e){
            throw new WikapidiaException(e);
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
}
