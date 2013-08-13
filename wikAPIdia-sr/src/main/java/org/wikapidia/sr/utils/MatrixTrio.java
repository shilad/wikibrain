package org.wikapidia.sr.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.normalize.IdentityNormalizer;
import org.wikapidia.sr.normalize.Normalizer;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A convenience container for trio of matrices needed for SR metrics:
 * 1. The feature matrix
 * 2. The transpose of the feature matrix
 * 3. The cosimilarity matrix.

 * @author Shilad Sen
 */
public class MatrixTrio {
    private static final Logger LOG = Logger.getLogger(MatrixTrio.class.getName());

    public static final String COSIMILARITY_MATRIX = "cosimilarityMatrix";
    public static final String FEATURE_TRANSPOSE_MATRIX = "featureTransposeMatrix";
    public static final String FEATURE_MATRIX = "featureMatrix";

    private File dir;

    private SparseMatrix featureMatrix = null;
    private SparseMatrix featureTransposeMatrix = null;
    private SparseMatrix cosimilarityMatrix = null;

    public MatrixTrio(File dir) {
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
}
