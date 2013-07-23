package org.wikapidia.sr;

import edu.emory.mathcs.backport.java.util.Arrays;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.IdentityNormalizer;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.pairwise.PairwiseMilneWittenSimilarity;
import org.wikapidia.sr.pairwise.PairwiseSimilarityWriter;
import org.wikapidia.sr.pairwise.SRFeatureMatrixWriter;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public abstract class BaseUniversalSRMetric implements UniversalSRMetric{
    private static final Logger LOG = Logger.getLogger(BaseUniversalSRMetric.class.getName());
    protected int numThreads = Runtime.getRuntime().availableProcessors();
    protected UniversalPageDao universalPageDao;
    protected Disambiguator disambiguator;
    protected int algorithmId;

    private Normalizer mostSimilarNormalizer = new IdentityNormalizer();
    private Normalizer similarityNormalizer = new IdentityNormalizer();

    protected SparseMatrix mostSimilarUniversalMatrix;

    public BaseUniversalSRMetric(Disambiguator disambiguator, UniversalPageDao universalPageDao, int algorithmId){
        this.universalPageDao = universalPageDao;
        this.disambiguator = disambiguator;
        this.algorithmId = algorithmId;
    }

    @Override
    public void setMostSimilarUniversalMatrix(SparseMatrix matrix) {
        this.mostSimilarUniversalMatrix = matrix;
    }

    public boolean hasCachedMostSimilarUniversal(int wpId){
        boolean hasCached;
        try {
            hasCached = mostSimilarUniversalMatrix != null && mostSimilarUniversalMatrix.getRow(wpId) != null;
        } catch (IOException e) {
            return false;
        }
        return hasCached;
    }

    public SRResultList getCachedMostSimilarUniversal(int wpId, int numResults, TIntSet validIds){
        if (!hasCachedMostSimilarUniversal(wpId)){
            return null;
        }
        SparseMatrixRow row;
        try {
            row = mostSimilarUniversalMatrix.getRow(wpId);
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
        LocalId similar1 = disambiguator.disambiguate(phrase1, context);
        context.clear();
        context.add(phrase1);
        LocalId similar2 = disambiguator.disambiguate(phrase2, context);
        if (similar1==null|| similar2==null){
            return new SRResult(Double.NaN);
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
        LocalId localId = disambiguator.disambiguate(phrase,null);
        if (localId == null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult(Double.NaN));
            return resultList;
        }
        int uId = universalPageDao.getUnivPageId(localId.asLocalPage(),algorithmId);
        UniversalPage up = universalPageDao.getById(uId,algorithmId);
        return mostSimilar(up,maxResults);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException {
        LocalId localId = disambiguator.disambiguate(phrase,null);
        if (localId == null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult(Double.NaN));
            return resultList;
        }
        int uId = universalPageDao.getUnivPageId(localId.asLocalPage(),algorithmId);
        UniversalPage up = universalPageDao.getById(uId,algorithmId);
        return mostSimilar(up,maxResults,validIds);
    }

    protected void ensureSimilarityTrained(){
        if(!similarityNormalizer.isTrained()){
            throw new IllegalStateException("Model default similarity has not been trained.");
        }
    }

    protected void ensureMostSimilarTrained(){
        if(!mostSimilarNormalizer.isTrained()){
            throw new IllegalStateException("Model default mostSimilar has not been trained.");
        }
    }

    protected SRResult normalize(SRResult sr){
        ensureSimilarityTrained();
        sr.value=similarityNormalizer.normalize(sr.value);
        return sr;
    }

    protected SRResultList normalize(SRResultList srl){
        ensureMostSimilarTrained();
        return mostSimilarNormalizer.normalize(srl);
    }

    @Override
    public void write(String path) throws IOException {
        ObjectOutputStream oop = new ObjectOutputStream(
                new FileOutputStream(path + getName() + "-" + algorithmId + "-mostSimilarNormalizer")
        );
        oop.writeObject(mostSimilarNormalizer);
        oop.flush();
        oop.close();

        oop = new ObjectOutputStream(
                new FileOutputStream(path + getName() + "-" + algorithmId + "-similarityNormalizer")
        );
        oop.writeObject(similarityNormalizer);
        oop.flush();
        oop.close();
    }

    @Override
    public void read(String path) throws IOException, ClassNotFoundException {
        ObjectInputStream oip = new ObjectInputStream(
                new FileInputStream(path + getName() + "-" + algorithmId + "-mostSimilarNormalizer")
        );
        this.mostSimilarNormalizer = (Normalizer)oip.readObject();
        oip.close();

        oip = new ObjectInputStream(
                new FileInputStream(path + getName() + "-" + algorithmId + "-similarityNormalizer")
        );
        this.similarityNormalizer = (Normalizer)oip.readObject();
        oip.close();
    }

    @Override
    public void trainSimilarity(final Dataset dataset) throws DaoException{
        final Normalizer trainee = similarityNormalizer;
        similarityNormalizer = new IdentityNormalizer();
        ParallelForEach.loop(dataset.getData(), numThreads, new Procedure<KnownSim>() {
            public void call(KnownSim ks) throws IOException, DaoException {
                LocalString ls1 = new LocalString(ks.language,ks.phrase1);
                LocalString ls2 = new LocalString(ks.language,ks.phrase2);
                SRResult sim = similarity(ls1,ls2, false);
                trainee.observe(sim.getValue(), ks.similarity);

            }
        },1);
        trainee.observationsFinished();
        similarityNormalizer = trainee;
        LOG.info("trained most similarityNormalizer for " + getName() + ": " + trainee.dump());
    }

    @Override
    public void trainMostSimilar(final Dataset dataset, final int numResults, final TIntSet validIds) throws DaoException{
        final Normalizer trainee = mostSimilarNormalizer;
        mostSimilarNormalizer = new IdentityNormalizer();
        ParallelForEach.loop(dataset.getData(), numThreads, new Procedure<KnownSim>() {
            public void call(KnownSim ks) throws DaoException {
                ks.maybeSwap();
                List<LocalString> localStrings = new ArrayList<LocalString>();
                localStrings.add(new LocalString(ks.language, ks.phrase1));
                localStrings.add(new LocalString(ks.language, ks.phrase2));
                List<LocalId> ids = disambiguator.disambiguate(localStrings, null);
                int pageId1 = universalPageDao.getUnivPageId(ids.get(0).asLocalPage(), algorithmId);
                int pageId2 = universalPageDao.getUnivPageId(ids.get(1).asLocalPage(),algorithmId);
                UniversalPage page = universalPageDao.getById(pageId1,algorithmId);
                if (page != null) {
                    SRResultList dsl = mostSimilar(page, numResults, validIds);
                    if (dsl != null) {
                        trainee.observe(dsl, dsl.getIndexForId(pageId2), ks.similarity);
                    }
                }
            }
        }, 1);
        trainee.observationsFinished();
        mostSimilarNormalizer = trainee;
        LOG.info("trained most similar normalizer for " + getName() + ": " + trainee.dump());
    }

    @Override
    public void setMostSimilarNormalizer(Normalizer n){
        mostSimilarNormalizer = n;
    }

    @Override
    public void setSimilarityNormalizer(Normalizer n){
        similarityNormalizer = n;
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
                        false).getValue();
                }
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(LocalString[] rowPhrases, LocalString[] colPhrases) throws IOException, DaoException {
        int rowIds[] = new int[rowPhrases.length];
        int colIds[] = new int[colPhrases.length];
        List<LocalId> rowLocalIds = disambiguator.disambiguate(Arrays.asList(rowPhrases),new HashSet<LocalString>(Arrays.asList(colPhrases)));
        List<LocalId> colLocalIds = disambiguator.disambiguate(Arrays.asList(colPhrases),new HashSet<LocalString>(Arrays.asList(rowPhrases)));
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
    public double[][] cosimilarity(LocalString[] phrases) throws IOException, DaoException {
        int ids[] = new int[phrases.length];
        List<LocalId> localIds = disambiguator.disambiguate(Arrays.asList(phrases), null);
        for (int i=0; i<phrases.length; i++){
            ids[i] = universalPageDao.getUnivPageId(localIds.get(i).asLocalPage(),algorithmId);
        }
        return cosimilarity(ids);
    }

    @Override
    public int getAlgorithmId() {
        return this.algorithmId;
    }

    @Override
    public void writeCosimilarity(String path, int numThreads, int maxHits) throws IOException, DaoException, WikapidiaException, InterruptedException {
        path = path + getName()+"-" + algorithmId;
        SRFeatureMatrixWriter featureMatrixWriter = new SRFeatureMatrixWriter(path, this);
        DaoFilter pageFilter = new DaoFilter().setAlgorithmIds(algorithmId);
        Iterable<UniversalPage> universalPages = universalPageDao.get(pageFilter);
        TIntSet pageIds = new TIntHashSet();
        for (UniversalPage page : universalPages) {
            if (page != null) {
                pageIds.add(page.getUnivId());
            }
        }

        featureMatrixWriter.writeFeatureVectors(pageIds.toArray(), 4);
        PairwiseMilneWittenSimilarity pairwise = new PairwiseMilneWittenSimilarity(path);
        PairwiseSimilarityWriter pairwiseSimilarityWriter = new PairwiseSimilarityWriter(path,pairwise);
        pairwiseSimilarityWriter.writeSims(pageIds.toArray(),numThreads,maxHits);
        mostSimilarUniversalMatrix = new SparseMatrix(new File(path+"-cosimilarity"));
    }
}
