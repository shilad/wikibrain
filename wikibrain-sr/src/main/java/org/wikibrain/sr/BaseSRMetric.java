package org.wikibrain.sr;

import com.typesafe.config.Config;

import java.util.*;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.matrix.*;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.disambig.SimilarityDisambiguator;
import org.wikibrain.sr.normalize.IdentityNormalizer;
import org.wikibrain.sr.normalize.Normalizer;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.utils.SrNormalizers;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.utils.WpThreadUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class provides many useful building blocks for Monolingual SR Metrics.
 */
public abstract class BaseSRMetric implements SRMetric {
    private static Logger LOG = LoggerFactory.getLogger(BaseSRMetric.class);

    private final String name;
    private final Language language;

    private File dataDir;
    private Disambiguator disambiguator;
    private LocalPageDao localPageDao;

    private boolean shouldReadNormalizers = true;
    private SrNormalizers normalizers;

    private boolean buildMostSimilarCache = false;
    private SparseMatrix mostSimilarCache = null;
    private TIntSet mostSimilarCacheRowIds = null;


    // the number of senses to consider for each phrase
    private int numSenses = 5;

    /**
     * Returns properties about the metric.
     */
    public static class SRConfig {
        // minimum and maximum scores BEFORE normalization
        public float minScore = -1.1f;
        public float maxScore = +1.1f;
    }

    public BaseSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig) {
        this.name = name;
        this.language = language;
        this.disambiguator = disambig;
        this.localPageDao = dao;
        this.normalizers =  new SrNormalizers();
    }

    public abstract SRConfig getConfig();

    @Override
    public File getDataDir() {
        return dataDir;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setDataDir(File dir) {
        this.dataDir= dir;
    }

    @Override
    public void setMostSimilarNormalizer(Normalizer n){
        normalizers.setMostSimilarNormalizer(n);
    }

    @Override
    public void setSimilarityNormalizer(Normalizer n){
        normalizers.setSimilarityNormalizer(n);
    }

    @Override
    public boolean similarityIsTrained() {
        return normalizers.getSimilarityNormalizer().isTrained();
    }

    @Override
    public boolean mostSimilarIsTrained() {
        return normalizers.getMostSimilarNormalizer().isTrained();
    }

    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureSimilarityTrained() {
        if (!similarityIsTrained()) {
            throw new IllegalStateException("Model similarity has not been trained.");
        }
    }

    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureMostSimilarTrained() {
        if (!mostSimilarIsTrained()) {
            throw new IllegalStateException("Model mostSimilar has not been trained.");
        }
    }

    /**
     * Use the language-specific similarity normalizer to normalize a similarity if it exists.
     * Otherwise use the default similarity normalizer if it's available.
     * @param sr
     * @return
     */
    protected SRResult normalize(SRResult sr) {
        sr.score=normalize(sr.score);
        return sr;
    }

    /**
     * Use the language-specific most similar normalizer to normalize a similarity if it exists.
     * Otherwise use the default most similar normalizer if it's available.
     * @param srl
     * @return
     */
    protected SRResultList normalize(SRResultList srl) {
        ensureMostSimilarTrained();
        return normalizers.getMostSimilarNormalizer().normalize(srl);
    }

    protected double normalize (double score){
        ensureSimilarityTrained();
        return normalizers.getSimilarityNormalizer().normalize(score);
    }

    @Override
    public void write() throws IOException {
        WpIOUtils.mkdirsQuietly(dataDir);
        normalizers.write(dataDir);
    }

    public void setReadNormalizers(boolean shouldRead) {
        this.shouldReadNormalizers = shouldRead;
    }

    @Override
    public void read() throws IOException {
        if (!dataDir.isDirectory()) {
            LOG.warn("directory " + dataDir + " does not exist; cannot read files");
            return;
        }
        if (shouldReadNormalizers && normalizers.hasReadableNormalizers(dataDir)) {
            normalizers.read(dataDir);
        }
        IOUtils.closeQuietly(mostSimilarCache);
        if (getMostSimilarMatrixPath().isFile()) {
            mostSimilarCache = new SparseMatrix(getMostSimilarMatrixPath());
        }
    }

    @Override
    public synchronized void trainSimilarity(Dataset dataset) throws DaoException {
        if (!dataset.getLanguage().equals(getLanguage())) {
            throw new IllegalArgumentException("SR metric has language " + getLanguage() + " but dataset has language " + dataset.getLanguage());
        }
        normalizers.trainSimilarity(this, dataset);
    }

    @Override
    public synchronized void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds){
        if (!dataset.getLanguage().equals(getLanguage())) {
            throw new IllegalArgumentException("SR metric has language " + getLanguage() + " but dataset has language " + dataset.getLanguage());
        }
        normalizers.trainMostSimilar(this, disambiguator, dataset, validIds, numResults);
        try {
            if (buildMostSimilarCache) {
                writeMostSimilarCache(numResults, mostSimilarCacheRowIds, validIds);
            }
        } catch (Exception e) {
            LOG.error("writing most similar cache failed:", e);
        }
    }

    @Override
    public abstract SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException;

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        Language language = getLanguage();
        List<LocalString> phrases = Arrays.asList(
                new LocalString(language, phrase1),
                new LocalString(language, phrase2));
//        debugSimilarityDisambiguator(phrases);
        List<LocalId> resolutions =  disambiguator.disambiguateTop(phrases, null);
        if (resolutions.get(0) == null || resolutions.get(1) == null) {
            return new SRResult();
        }
//        LocalPage lp1 = localPageDao.getById(language, resolutions.get(0).getId());
//        LocalPage lp2 = localPageDao.getById(language, resolutions.get(1).getId());
//        System.out.println("resolved " + phrase1 + ", " + phrase2 + " to " + lp1 + ", " + lp2);
        return similarity(resolutions.get(0).getId(), resolutions.get(1).getId(), explanations);
    }

    private void debugSimilarityDisambiguator(List<LocalString> phrases) throws DaoException {
        String last = null;
        boolean same = true;
        StringBuffer b = new StringBuffer("results for " + phrases.get(0).getString() + ", " + phrases.get(1).getString() + "\n");
        for (SimilarityDisambiguator.Criteria c : SimilarityDisambiguator.Criteria.values()) {
            if (c == SimilarityDisambiguator.Criteria.SIMILARITY) {
                continue;   // weird, so skip for now.
            }
            List<LocalId> resolutions;
            synchronized (disambiguator) {
                ((SimilarityDisambiguator)disambiguator).setCriteria(c);
                resolutions = disambiguator.disambiguateTop(phrases, null);
            }
            String page1 = resolutions.get(0) == null ? "null" : localPageDao.getById(language, resolutions.get(0).getId()).toString();
            String page2 = resolutions.get(1) == null ? "null" : localPageDao.getById(language, resolutions.get(1).getId()).toString();
            b.append("\t" + c + ": " + page1 + ",  " + page2 + "\n");
            if (last == null)
                last = page1+page2;
            if (!last.equals(page1+page2)) {
                same = false;
            }
        }
        if (!same) {
            System.out.println(b.toString());
        }

    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
        return mostSimilar(pageId, maxResults, null);
    }

    @Override
    public abstract SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException;

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults) throws DaoException {
        LocalId similar = disambiguator.disambiguateTop(new LocalString(getLanguage(), phrase), null);
        if (similar==null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult());
            return resultList;
        }
        return mostSimilar(similar.getId(), maxResults);
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException{
        LocalId similar = disambiguator.disambiguateTop(new LocalString(getLanguage(), phrase), null);
        if (similar==null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult());
            return resultList;
        }
        return mostSimilar(similar.getId(), maxResults,validIds);
    }

    @Override
    public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds) throws DaoException {
        double[][] cos = new double[wpRowIds.length][wpColIds.length];
        for (int i=0; i<wpRowIds.length; i++){
            for (int j=0; j<wpColIds.length; j++){
                if (wpRowIds[i]==wpColIds[j]){
                    cos[i][j]=normalize(1.0);
                } else{
                    cos[i][j]=similarity(wpRowIds[i], wpColIds[j], false).getScore();
                }
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(String[] rowPhrases, String[] colPhrases) throws DaoException {
        double[][] cos = new double[rowPhrases.length][colPhrases.length];
        for (int i=0; i<rowPhrases.length; i++){
            for (int j=0; j<colPhrases.length; j++){
                if (rowPhrases[i].equals(colPhrases[j])){
                    cos[i][j]=normalize(1.0);
                }
                else{
                    cos[i][j]=similarity(rowPhrases[i],colPhrases[j],false).getScore();
                }
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(int[] ids) throws DaoException {
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=normalize(1.0);
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]=similarity(ids[i], ids[j], false).getScore();
                cos[j][i]=cos[i][j];
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(String[] phrases) throws DaoException {
        int ids[] = new int[phrases.length];
        List<LocalString> localStringList = new ArrayList<LocalString>();
        for (String phrase : phrases){
            localStringList.add(new LocalString(getLanguage(), phrase));
        }
        List<LocalId> localIds = disambiguator.disambiguateTop(localStringList, null);
        for (int i=0; i<phrases.length; i++){
            ids[i] = localIds.get(i).getId();
        }
        return cosimilarity(ids);
    }

    /**
     * If the cache exists, and contains at least numResults valid ids for the requested id, return it.
     * Otherwise, return null.
     *
     * @param wpId
     * @param numResults
     * @param validIds
     * @return
     * @throws DaoException
     */
    protected SRResultList getCachedMostSimilar(int wpId, int numResults, TIntSet validIds) throws DaoException {
        if (mostSimilarCache == null) {
            return null;
        }
        MatrixRow row = null;
        try {
            row = mostSimilarCache.getRow(wpId);
        } catch (IOException e) {
            throw new DaoException(e);
        }
        if (row == null || row.getNumCols() < numResults ) {
            return null;
        }
        Leaderboard leaderboard = new Leaderboard(numResults);
        for (int i=0; i<row.getNumCols() ; i++){
            int wpId2 = row.getColIndex(i);
            if (validIds == null || validIds.contains(wpId2)){
                leaderboard.tallyScore(wpId2, row.getColValue(i));
            }
        }
        SRResultList results = leaderboard.getTop();
        if (results.numDocs() < numResults) {
            return null;
        }
        return results;
    }

    public void writeMostSimilarCache(int maxHits) throws IOException, DaoException, WikiBrainException {
        writeMostSimilarCache(maxHits, null, null);
    }

    /**
     * Creates and writes a sparse matrix that records the top-k results for every page.
     * @param maxHits
     * @param rowIds
     * @param colIds
     * @throws IOException
     * @throws DaoException
     * @throws WikiBrainException
     */
    public void writeMostSimilarCache(final int maxHits, TIntSet rowIds, TIntSet colIds) throws IOException, DaoException, WikiBrainException{

        TIntSet allPageIds = null;
        // Get all page ids
        if (rowIds == null || colIds == null) {
            DaoFilter pageFilter = new DaoFilter()
                    .setLanguages(getLanguage())
                    .setNameSpaces(NameSpace.ARTICLE)
                    .setDisambig(false)
                    .setRedirect(false);
            Iterable<LocalPage> localPages = localPageDao.get(pageFilter);
            allPageIds = new TIntHashSet();
            for (LocalPage page : localPages) {
                if (page != null) {
                    allPageIds.add(page.getLocalId());
                }
            }
        }
        if (rowIds == null) rowIds = allPageIds;
        if (colIds == null) colIds = allPageIds;

        getDataDir().mkdirs();
        IOUtils.closeQuietly(mostSimilarCache);


        SRConfig config = getConfig();
        final AtomicInteger idCounter = new AtomicInteger();
        final AtomicLong cellCounter = new AtomicLong();
        ValueConf vconf = new ValueConf(config.minScore, config.maxScore);

        final SparseMatrixWriter writer = new SparseMatrixWriter(getMostSimilarMatrixPath(), vconf);
        final TIntSet colIdSet = colIds == null ? null : new TIntHashSet(colIds);


        Normalizer simNormalizer = getSimilarityNormalizer();
        Normalizer mostSimNormalizer = getMostSimilarNormalizer();
        setMostSimilarNormalizer(new IdentityNormalizer());
        setSimilarityNormalizer(new IdentityNormalizer());
        try {
            ParallelForEach.loop(
                    Arrays.asList(ArrayUtils.toObject(rowIds.toArray())),
                    WpThreadUtils.getMaxThreads(),
                    new Procedure<Integer>() {
                        public void call(Integer wpId) throws IOException, DaoException {
                            writeSim(writer, wpId, colIdSet, maxHits, idCounter, cellCounter);
                        }
                    }, Integer.MAX_VALUE);
        } finally {
            setSimilarityNormalizer(simNormalizer);
            setMostSimilarNormalizer(mostSimNormalizer);
        }

        LOG.info("wrote " + cellCounter.get() + " non-zero similarity cells");
        writer.finish();
        mostSimilarCache = new SparseMatrix(getMostSimilarMatrixPath());
    }

    protected File getMostSimilarMatrixPath() {
        return new File(getDataDir(), "mostSimilar.matrix");
    }


    private void writeSim(SparseMatrixWriter writer, Integer wpId, TIntSet colIds, int maxSimsPerDoc, AtomicInteger idCounter, AtomicLong cellCounter) throws IOException, DaoException {
        if (idCounter.incrementAndGet() % 10000 == 0) {
            LOG.info("finding matches for page " + idCounter.get());
        }
        SRResultList scores = mostSimilar(wpId, maxSimsPerDoc, colIds);
        if (scores != null) {
            int ids[] = scores.getIds();
            cellCounter.getAndIncrement();
            writer.writeRow(new SparseMatrixRow(writer.getValueConf(), wpId, ids, scores.getScoresAsFloat()));
        }
    }

    public Language getLanguage() {
        return language;
    }

    public Disambiguator getDisambiguator() {
        return disambiguator;
    }

    public LocalPageDao getLocalPageDao() {
        return localPageDao;
    }

    @Override
    public Normalizer getMostSimilarNormalizer() {
        return normalizers.getMostSimilarNormalizer();
    }

    @Override
    public Normalizer getSimilarityNormalizer() {
        return normalizers.getSimilarityNormalizer();
    }

    public SparseMatrix getMostSimilarCache() {
        return mostSimilarCache;
    }

    public void clearMostSimilarCache() {
        IOUtils.closeQuietly(mostSimilarCache);
        FileUtils.deleteQuietly(getMostSimilarMatrixPath());
        mostSimilarCache = null;
    }

    public void setBuildMostSimilarCache(boolean buildMostSimilarCache) {
        this.buildMostSimilarCache = buildMostSimilarCache;
    }

    public void setMostSimilarCacheRowIds(TIntSet rowIds) {
        this.mostSimilarCacheRowIds = rowIds;
    }

    protected static void configureBase(Configurator configurator, BaseSRMetric sr, Config config) throws ConfigurationException {
        Config rootConfig = configurator.getConf().get();

        File path = new File(rootConfig.getString("sr.metric.path"));
        sr.setDataDir(FileUtils.getFile(path, sr.getName(), sr.getLanguage().getLangCode()));

        // initialize normalizers
        sr.setSimilarityNormalizer(configurator.get(Normalizer.class, config.getString("similaritynormalizer")));
        sr.setMostSimilarNormalizer(configurator.get(Normalizer.class, config.getString("mostsimilarnormalizer")));

        boolean isTraining = rootConfig.getBoolean("sr.metric.training");
        if (isTraining) {
            sr.setReadNormalizers(false);
        }
        if (config.hasPath("buildMostSimilarCache")) {
            sr.setBuildMostSimilarCache(config.getBoolean("buildMostSimilarCache"));
        }

        try {
            sr.read();
        } catch (IOException e){
            throw new ConfigurationException(e);
        }
        LOG.info("finished base configuration of metric " + sr.getName());
    }
}
