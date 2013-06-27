package org.wikapidia.metrics;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.metrics.normalize.IdentityNormalizer;
import org.wikapidia.metrics.normalize.Normalizer;


import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public abstract class BaseSimilarityMetric implements SimilarityMetric{
    private static Logger LOG = Logger.getLogger(BaseSimilarityMetric.class.getName());
    protected int numThreads = Runtime.getRuntime().availableProcessors();

    private Normalizer mostSimilarNormalizer = new IdentityNormalizer();
    private Normalizer similarityNormalizer = new IdentityNormalizer();

    protected SparseMatrix mostSimilarUniversalMatrix;
    protected Map<Language,SparseMatrix> mostSimilarLocalMatrices;


    public BaseSimilarityMetric(){

    }

    public void setMostSimilarUniversalMatrix(SparseMatrix matrix) {
        this.mostSimilarUniversalMatrix = matrix;
    }

    public void setMostSimilarLocalMatrices(Map<Language,SparseMatrix> mostSimilarLocalMatrices){
        this.mostSimilarLocalMatrices=mostSimilarLocalMatrices;
    }

    public void setMostSimilarLocalMatrix(Language language, SparseMatrix sparseMatrix){
        this.mostSimilarLocalMatrices.put(language,sparseMatrix);
    }

    public boolean hasCachedMostSimilarUniversal(int wpId) throws IOException {
        return mostSimilarUniversalMatrix != null && mostSimilarUniversalMatrix.getRow(wpId) != null;
    }

    public boolean hasCachedMostSimilarLocal(Language language, int wpId) throws  IOException{
        return mostSimilarLocalMatrices != null && mostSimilarLocalMatrices.containsKey(language)
                && mostSimilarLocalMatrices.get(language).getRow(wpId) != null;
    }

    public SRResultList getCachedMostSimilarUniversal(int wpId, int numResults, TIntSet validIds) throws IOException {
        if (!hasCachedMostSimilarUniversal(wpId)){
            return null;
        }
        SparseMatrixRow row = mostSimilarUniversalMatrix.getRow(wpId);
        int n = 0;
        SRResultList srl = new SRResultList(row.getNumCols());
        for (int i = 0;i < row.getNumCols() &&  n < numResults; i++) {
            int wpId2 = row.getColIndex(i);
            if (validIds == null || validIds.contains(wpId2)) {
                srl.set(n++, row.getColIndex(i), row.getColValue(i));
            }
        }
        srl.truncate(n);
        return srl;
    }

    public SRResultList getCachedMostSimilarLocal(Language language, int wpId, int numResults, TIntSet validIds) throws IOException {
        if (!hasCachedMostSimilarLocal(language, wpId)){
            return null;
        }
        SparseMatrixRow row = mostSimilarLocalMatrices.get(language).getRow(wpId);
        int n = 0;
        SRResultList srl = new SRResultList(row.getNumCols());
        for (int i=0; i<row.getNumCols() && n < numResults; i++){
            int wpId2 = row.getColIndex(i);
            if (validIds == null || validIds.contains(wpId2)){
                srl.set(n++, row.getColIndex(i), row.getColValue(i));
            }
        }
        srl.truncate(n);
        return srl;
    }

    /**
     * Normalizers translate similarity scores to more meaningful values.
     * @param n
     */
    public void setMostSimilarNormalizer(Normalizer n){
        mostSimilarNormalizer = n;
    }
}
