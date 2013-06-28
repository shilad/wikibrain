package org.wikapidia.metrics;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.metrics.normalize.IdentityNormalizer;
import org.wikapidia.metrics.normalize.Normalizer;
import org.wikapidia.metrics.utils.KnownSim;


import java.io.File;
import java.io.IOException;
import java.util.List;
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


    public void setSimilarityNormalizer(Normalizer similarityNormalizer) {
        this.similarityNormalizer = similarityNormalizer;
    }

    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureSimilarityTrained() {
        if (!similarityNormalizer.isTrained()) {
            throw new IllegalStateException("Model similarity has not been trained.");
        }
    }
    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureMostSimilarTrained() {
        if (!mostSimilarNormalizer.isTrained()) {
            throw new IllegalStateException("Model mostSimilar has not been trained.");
        }
    }

    /**
     * Use the similarityNormalizer to normalize a similarity if it's available.
     * @param sim
     * @return
     */
    protected double normalize(double sim) {
        ensureSimilarityTrained();
        return similarityNormalizer.normalize(sim);
    }

    /**
     * Use the mostSimilarNormalizer to normalize a list of score if possible.
     * @param srl
     * @return
     */
    protected SRResultList normalize(SRResultList srl) {
        ensureMostSimilarTrained();
        return mostSimilarNormalizer.normalize(srl);
    }

    public void setNumThreads(int n) {
        this.numThreads = n;
    }



    @Override
    public abstract SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations);

    @Override
    public abstract SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations);

    @Override
    public abstract SRResult localSimilarity(String phrase1, String phrase2, Language language, boolean explanations);

    @Override
    public abstract SRResult universalSimilarity(String phrase1, String phrase2, Language language, boolean explanations);

    @Override
    public abstract SRResult universalSimilarity(String phrase1, Language language1, String phrase2, Language language2, boolean explanations);

    @Override
    public abstract SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations);

    @Override
    public abstract SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds);

    @Override
    public abstract SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations);

    @Override
    public abstract SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations, TIntSet validIds);

    @Override
    public abstract SRResultList localMostSimilar(String phrase, Language language, int maxResults, boolean explanations);

    @Override
    public abstract SRResultList localMostSimilar(String phrase, Language language, int maxResults, boolean explanations, TIntSet validIds);

    @Override
    public abstract SRResultList universalMostSimilar(String phrase, Language language, int maxResults, boolean explanations);

    @Override
    public abstract SRResultList universalMostSimilar(String phrase, Language language, int maxResults, boolean explanations, TIntSet validIds);

    @Override
    public abstract void write(File directory) throws IOException;

    @Override
    public abstract void read(File directory) throws IOException;

    @Override
    public abstract void trainSimilarity(List<KnownSim> labeled);

    @Override
    public abstract void trainMostSimilar(List<KnownSim> labeled, int numResults, TIntSet validIds);

    @Override
    public abstract double[][] localCosimilarity(int[] wpRowIds, int[] wpColIds, Language language) throws IOException;

    @Override
    public abstract double[][] localCosimilarity(String[] rowPhrases, String[] colPhrases, Language language) throws IOException;

    @Override
    public double[][] localCosimilarity(int[] ids, Language language) throws IOException{
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=1;
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]=localSimilarity(ids[i],ids[j], language, false).getValue();
            }
        }
        for (int i=1; i<ids.length; i++){
            for (int j=i-1; j>-1; j--){
                cos[i][j]=cos[j][i];
            }
        }
        return cos;
    }

    @Override
    public abstract double[][] localCosimilarity(String[] phrases, Language language) throws IOException;

    @Override
    public abstract double[][] universalCosimilarity(int[] wpRowIds, int[] wpColIds) throws IOException;

    @Override
    public abstract double[][] universalCosimilarity(String[] rowPhrases, Language[] rowLanguages, String[] colPhrases, Language[] colLanguages) throws IOException;

    @Override
    public double[][] universalCosimilarity(int[] ids) throws IOException {
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=1;
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]=universalSimilarity(ids[i],ids[j], false).getValue();
            }
        }
        for (int i=1; i<ids.length; i++){
            for (int j=i-1; j>-1; j--){
                cos[i][j]=cos[j][i];
            }
        }
        return cos;
    }

    @Override
    public abstract double[][] universalCosimilarity(String[] phrases, Language[] languages) throws IOException;
}
