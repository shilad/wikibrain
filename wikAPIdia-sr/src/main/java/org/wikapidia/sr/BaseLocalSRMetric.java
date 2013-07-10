package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.IdentityNormalizer;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.KnownSim;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public abstract class BaseLocalSRMetric implements LocalSRMetric {
    private static Logger LOG = Logger.getLogger(BaseLocalSRMetric.class.getName());
    protected int numThreads = Runtime.getRuntime().availableProcessors();
    protected Disambiguator disambiguator;
    protected LocalPageDao pageHelper;


    private Normalizer defaultMostSimilarNormalizer = new IdentityNormalizer();
    private Normalizer defaultSimilarityNormalizer = new IdentityNormalizer();
    private Map<Language, Normalizer> similarityNormalizers;
    private Map<Language, Normalizer> mostSimilarNormalizers;

    protected Map<Language,SparseMatrix> mostSimilarLocalMatrices;

    public void setMostSimilarLocalMatrices(Map<Language,SparseMatrix> mostSimilarLocalMatrices){
        this.mostSimilarLocalMatrices=mostSimilarLocalMatrices;
    }

    public void setMostSimilarLocalMatrix(Language language, SparseMatrix sparseMatrix){
        this.mostSimilarLocalMatrices.put(language,sparseMatrix);
    }

    public boolean hasCachedMostSimilarLocal(Language language, int wpId) {
        boolean hasCached;
        try {
            hasCached = mostSimilarLocalMatrices != null && mostSimilarLocalMatrices.containsKey(language)
                && mostSimilarLocalMatrices.get(language).getRow(wpId) != null;
        } catch (IOException e){
            return false;
        }
        return hasCached;
    }

    public SRResultList getCachedMostSimilarLocal(Language language, int wpId, int numResults, TIntSet validIds) {
        if (!hasCachedMostSimilarLocal(language, wpId)){
            return null;
        }
        SparseMatrixRow row;
        try {
            row = mostSimilarLocalMatrices.get(language).getRow(wpId);
        } catch (IOException e){
            return null;
        }
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
    public void setDefaultMostSimilarNormalizer(Normalizer n){
        defaultMostSimilarNormalizer = n;
    }

    public void setDefaultSimilarityNormalizer(Normalizer defaultSimilarityNormalizer) {
        this.defaultSimilarityNormalizer = defaultSimilarityNormalizer;
    }

    public void setMostSimilarNormalizer(Normalizer n, Language l){
        mostSimilarNormalizers.put(l,n);
    }

    public void setSimilarityNormalizer(Normalizer n, Language l){
        similarityNormalizers.put(l,n);
    }

    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureSimilarityTrained() {
        if (!defaultSimilarityNormalizer.isTrained()) {
            throw new IllegalStateException("Model default similarity has not been trained.");
        }
    }
    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureMostSimilarTrained() {
        if (!defaultMostSimilarNormalizer.isTrained()) {
            throw new IllegalStateException("Model default mostSimilar has not been trained.");
        }
    }

    /**
     * Use the language-specific similarity normalizer to normalize a similarity if it exists.
     * Otherwise use the default similarity normalizer if it's available.
     * @param sim
     * @param language
     * @return
     */
    protected double normalize(double sim, Language language) {
        if (similarityNormalizers.containsKey(language)){
            return similarityNormalizers.get(language).normalize(sim);
        }
        ensureSimilarityTrained();
        return defaultSimilarityNormalizer.normalize(sim);
    }

    /**
     * Use the language-specific most similar normalizer to normalize a similarity if it exists.
     * Otherwise use the default most similar normalizer if it's available.
     * @param srl
     * @param language
     * @return
     */
    protected SRResultList normalize(SRResultList srl, Language language) {
        if (similarityNormalizers.containsKey(language)){
            return similarityNormalizers.get(language).normalize(srl);
        }
        ensureMostSimilarTrained();
        return defaultMostSimilarNormalizer.normalize(srl);
    }

    public void setNumThreads(int n) {
        this.numThreads = n;
    }



    @Override
    public abstract SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException;



    @Override
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException {
        HashSet<LocalString> context = new HashSet<LocalString>();
        context.add(new LocalString(language,phrase2));
        LocalId similar1 = disambiguator.disambiguate(new LocalString(language, phrase1), context);
        context.clear();
        context.add(new LocalString(language,phrase1));
        LocalId similar2 = disambiguator.disambiguate(new LocalString(language,phrase2),context);
        return similarity(pageHelper.getById(language,similar1.getId()),
                pageHelper.getById(language,similar2.getId()),
                explanations);
    }

    @Override
    public abstract SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations) throws DaoException;

    @Override
    public abstract SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds) throws DaoException;

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations) throws DaoException {
        LocalId similar = disambiguator.disambiguate(phrase,null);
        return mostSimilar(similar.asLocalPage(), maxResults, explanations);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations, TIntSet validIds) throws DaoException {
        LocalId similar = disambiguator.disambiguate(phrase,null);
        return mostSimilar(similar.asLocalPage(), maxResults, explanations,validIds);
    }

    @Override
    public abstract void write(File directory) throws IOException;

    @Override
    public abstract void read(File directory) throws IOException;

    @Override
    public abstract void trainSimilarity(List<KnownSim> labeled);

    @Override
    public abstract void trainMostSimilar(List<KnownSim> labeled, int numResults, TIntSet validIds);

    @Override
    public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds, Language language) throws DaoException {
        double[][] cos = new double[wpRowIds.length][wpColIds.length];
        for (int i=0; i<wpRowIds.length; i++){
            for (int j=0; j<wpColIds.length; j++){
                if (wpRowIds[i]==wpColIds[j]){
                    cos[i][j]=1;
                }
                else{
                    cos[i][j]=similarity(
                            new LocalPage(language,wpRowIds[i],null,null),
                            new LocalPage(language,wpColIds[j],null,null),
                            false).getValue();
                }
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(String[] rowPhrases, String[] colPhrases, Language language) throws DaoException {
        double[][] cos = new double[rowPhrases.length][colPhrases.length];
        for (int i=0; i<rowPhrases.length; i++){
            for (int j=0; j<colPhrases.length; j++){
                if (rowPhrases[i].equals(colPhrases[j])){
                    cos[i][j]=1;
                }
                else{
                    cos[i][j]=similarity(rowPhrases[i],colPhrases[j],language, false).getValue();
                }
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(int[] ids, Language language) throws DaoException {
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=1;
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]=similarity(
                        new LocalPage(language, ids[i], null, null),
                        new LocalPage(language, ids[j], null, null),
                        false).getValue();
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
    public double[][] cosimilarity(String[] phrases, Language language) throws DaoException {
        int ids[] = new int[phrases.length];
        List<LocalString> localStringList = new ArrayList<LocalString>();
        for (String phrase : phrases){
            localStringList.add(new LocalString(language, phrase));
        }
        List<LocalId> localIds = disambiguator.disambiguate(localStringList, null);
        for (int i=0; i<phrases.length; i++){
            ids[i] = localIds.get(i).getId();
        }
        return cosimilarity(ids, language);
    }


}
