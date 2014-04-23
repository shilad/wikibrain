package org.wikibrain.sr;

import edu.emory.mathcs.backport.java.util.Arrays;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.IOUtils;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.matrix.SparseMatrixRow;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.normalize.Normalizer;
import org.wikibrain.sr.pairwise.MostSimilarCache;
import org.wikibrain.sr.pairwise.PairwiseSimilarity;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.utils.SrNormalizers;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public abstract class BaseUniversalSRMetric implements UniversalSRMetric{
    private static final Logger LOG = Logger.getLogger(BaseUniversalSRMetric.class.getName());
    protected UniversalPageDao universalPageDao;
    protected Disambiguator disambiguator;
    protected int algorithmId;

    private SrNormalizers normalizers = new SrNormalizers();

    private MostSimilarCache mostSimilarMatrices = null;

    public BaseUniversalSRMetric(Disambiguator disambiguator, UniversalPageDao universalPageDao, int algorithmId){
        this.universalPageDao = universalPageDao;
        this.disambiguator = disambiguator;
        this.algorithmId = algorithmId;
    }

    public boolean hasCachedMostSimilarUniversal(int conceptId) {
        try {
            return mostSimilarMatrices.getCosimilarityMatrix().getRow(conceptId) != null;
        } catch (IOException e) {
            throw new RuntimeException(e);  // should not happen
        }
    }

    public SRResultList getCachedMostSimilarUniversal(int wpId, int numResults, TIntSet validIds){
        if (!hasCachedMostSimilarUniversal(wpId)){
            return null;
        }
        SparseMatrixRow row;
        try {
            row = mostSimilarMatrices.getCosimilarityMatrix().getRow(wpId);
        } catch (IOException e) {
            return null;
        }
        Leaderboard leaderboard = new Leaderboard(numResults);
        for (int i=0; i<row.getNumCols() ; i++){
            int wpId2 = row.getColIndex(i);
            float value = row.getColValue(i);
            if (validIds == null || validIds.contains(wpId2)){
                leaderboard.tallyScore(wpId2, value);
            }
        }
        SRResultList results = leaderboard.getTop();
        results.sortDescending();
        return results;
    }

    @Override
    public abstract SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations) throws DaoException;

    @Override
    public SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations) throws DaoException {
        HashSet<LocalString> context = new HashSet<LocalString>();
        context.add(phrase2);
        LocalId similar1 = disambiguator.disambiguateTop(phrase1, context);
        context.clear();
        context.add(phrase1);
        LocalId similar2 = disambiguator.disambiguateTop(phrase2, context);
        if (similar1==null|| similar2==null){
            return new SRResult();
        }
        int uId1 = universalPageDao.getUnivPageId(similar1.asLocalPage(),algorithmId);
        UniversalPage up1 = universalPageDao.getById(uId1,algorithmId);
        int uId2 = universalPageDao.getUnivPageId(similar2.asLocalPage(),algorithmId);
        UniversalPage up2 = universalPageDao.getById(uId2,algorithmId);
        return similarity(up1,up2,explanations);
    }


    @Override
    public abstract SRResultList mostSimilar(UniversalPage page, int maxResults) throws DaoException;

    @Override
    public abstract SRResultList mostSimilar(UniversalPage page, int maxResults, TIntSet validIds) throws DaoException;

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws DaoException {
        LocalId localId = disambiguator.disambiguateTop(phrase, null);
        if (localId == null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult());
            return resultList;
        }
        int uId = universalPageDao.getUnivPageId(localId.asLocalPage(),algorithmId);
        UniversalPage up = universalPageDao.getById(uId,algorithmId);
        return mostSimilar(up,maxResults);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException {
        LocalId localId = disambiguator.disambiguateTop(phrase, null);
        if (localId == null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult());
            return resultList;
        }
        int uId = universalPageDao.getUnivPageId(localId.asLocalPage(),algorithmId);
        UniversalPage up = universalPageDao.getById(uId,algorithmId);
        return mostSimilar(up,maxResults,validIds);
    }

    protected void ensureSimilarityTrained(){
        if(!normalizers.getSimilarityNormalizer().isTrained()){
            throw new IllegalStateException("Model default similarity has not been trained.");
        }
    }

    protected void ensureMostSimilarTrained(){
        if(!normalizers.getMostSimilarNormalizer().isTrained()){
            throw new IllegalStateException("Model default mostSimilar has not been trained.");
        }
    }

    protected SRResult normalize(SRResult sr){
        ensureSimilarityTrained();
        sr.score =normalizers.getSimilarityNormalizer().normalize(sr.score);
        return sr;
    }

    protected SRResultList normalize(SRResultList srl){
        ensureMostSimilarTrained();
        return normalizers.getMostSimilarNormalizer().normalize(srl);
    }

    @Override
    public void write(String path) throws IOException {
        File dir = new File(path, getName());
        WpIOUtils.mkdirsQuietly(dir);
        normalizers.write(dir);
    }

    @Override
    public void read(String path) throws IOException {
        File dir = new File(path, getName());
        if (!dir.isDirectory()) {
            LOG.warning("directory " + dir + " does not exist; cannot read files");
            return;
        }
        if (normalizers.hasReadableNormalizers(dir)) {
            normalizers.read(dir);
        }
    }

    @Override
    public void trainSimilarity(final Dataset dataset) throws DaoException {
        normalizers.trainSimilarity(this, dataset);
    }

    @Override
    public void trainMostSimilar(final Dataset dataset, final int numResults, final TIntSet validIds) throws DaoException{
        normalizers.trainMostSimilar(this, disambiguator, universalPageDao, algorithmId, dataset, validIds, numResults);
    }

    @Override
    public void setMostSimilarNormalizer(Normalizer n){
        this.normalizers.setMostSimilarNormalizer(n);
    }

    @Override
    public void setSimilarityNormalizer(Normalizer n){
        this.normalizers.setSimilarityNormalizer(n);
    }

    @Override
    public double[][] cosimilarity(int[] rowIds, int[] colIds) throws IOException, DaoException {
        double[][] cos = new double[rowIds.length][colIds.length];
        for (int i=0; i<rowIds.length; i++){
            for (int j=0; j<colIds.length; j++){
                if (rowIds[i]==colIds[j]){
                    cos[i][j]=1;
                }
                else {
                    cos[i][j]=similarity(
                        new UniversalPage(rowIds[i], algorithmId),
                        new UniversalPage(colIds[j], algorithmId),
                        false).getScore();
                }
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(LocalString[] rowPhrases, LocalString[] colPhrases) throws IOException, DaoException {
        int rowIds[] = new int[rowPhrases.length];
        int colIds[] = new int[colPhrases.length];
        List<LocalId> rowLocalIds = disambiguator.disambiguateTop(Arrays.asList(rowPhrases), new HashSet<LocalString>(Arrays.asList(colPhrases)));
        List<LocalId> colLocalIds = disambiguator.disambiguateTop(Arrays.asList(colPhrases), new HashSet<LocalString>(Arrays.asList(rowPhrases)));
        for (int i=0; i<rowIds.length; i++){
            rowIds[i] = universalPageDao.getUnivPageId(rowLocalIds.get(i).asLocalPage(),algorithmId);
        }
        for (int i=0; i<colIds.length; i++){
            colIds[i] = universalPageDao.getUnivPageId(colLocalIds.get(i).asLocalPage(),algorithmId);
        }
        return cosimilarity(rowIds, colIds);
    }

    @Override
    public double[][] cosimilarity(int[] ids) throws IOException, DaoException {
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=1;
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]=similarity(
                        new UniversalPage(ids[i], 0),
                        new UniversalPage(ids[j], 0),
                        false).getScore();
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
    public double[][] cosimilarity(LocalString[] phrases) throws IOException, DaoException {
        int ids[] = new int[phrases.length];
        List<LocalId> localIds = disambiguator.disambiguateTop(Arrays.asList(phrases), null);
        for (int i=0; i<phrases.length; i++){
            ids[i] = universalPageDao.getUnivPageId(localIds.get(i).asLocalPage(),algorithmId);
        }
        return cosimilarity(ids);
    }

    @Override
    public int getAlgorithmId() {
        return this.algorithmId;
    }

    protected void writeCosimilarity(String parentDir, int maxHits, PairwiseSimilarity pairwise) throws IOException, DaoException, WikiBrainException{
        try {

            MostSimilarCache srm = new MostSimilarCache(this, pairwise, new File(parentDir, getName()));
//            path = path + getName()+"/matrix/" + algorithmId;
//            SRFeatureMatrixWriter featureMatrixWriter = new SRFeatureMatrixWriter(path, this);
            DaoFilter pageFilter = new DaoFilter().setAlgorithmIds(algorithmId);
            Iterable<UniversalPage> universalPages = universalPageDao.get(pageFilter);
            TIntSet pageIds = new TIntHashSet();
            for (UniversalPage page : universalPages) {
                if (page != null) {
                    pageIds.add(page.getUnivId());
                }
            }
            throw new InterruptedException();
//
//            featureMatrixWriter.writeFeatureVectors(pageIds.toArray(), 4);
//            pairwise.initMatrices(path);
//            PairwiseSimilarityWriter pairwiseSimilarityWriter = new PairwiseSimilarityWriter(path,pairwise);
//            pairwiseSimilarityWriter.writeSims(pageIds.toArray(),maxHits);
//            mostSimilarUniversalMatrix = new SparseMatrix(new File(path+"-cosimilarity"));
        }catch (InterruptedException e){
            throw new RuntimeException();
        }
    }

    protected void readCosimilarity (String parentDir, PairwiseSimilarity similarity) throws IOException{
        if (mostSimilarMatrices != null) {
            IOUtils.closeQuietly(mostSimilarMatrices);
            mostSimilarMatrices = null;
        }
        MostSimilarCache srm = new MostSimilarCache(this, similarity, new File(parentDir, getName()));
        if (srm.hasReadableMatrices()) {
            srm.read();
        }
        mostSimilarMatrices = srm;
    }
}
