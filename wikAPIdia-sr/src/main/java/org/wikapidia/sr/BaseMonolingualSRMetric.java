package org.wikapidia.sr;

import com.typesafe.config.Config;

import java.util.*;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.pairwise.MostSimilarCache;
import org.wikapidia.sr.pairwise.PairwiseCosineSimilarity;
import org.wikapidia.sr.pairwise.PairwiseSimilarity;
import org.wikapidia.sr.utils.SrNormalizers;
import org.wikapidia.utils.WpIOUtils;
import org.wikapidia.utils.WpThreadUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This abstract class provides many useful building blocks for Monolingual SR Metrics.
 */
public abstract class BaseMonolingualSRMetric implements MonolingualSRMetric {
    private static Logger LOG = Logger.getLogger(BaseMonolingualSRMetric.class.getName());

    private final String name;
    private final Language language;

    private File dataDir;
    private Disambiguator disambiguator;
    private LocalPageDao localPageDao;

    private boolean shouldReadNormalizers = true;
    private SrNormalizers normalizers;

    private MostSimilarCache mostSimilarCache = null;

    public BaseMonolingualSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig) {
        this.name = name;
        this.language = language;
        this.disambiguator = disambig;
        this.localPageDao = dao;
        this.normalizers =  new SrNormalizers();
    }

    public static class MetricConfig {
        /**
         * Whether or not the metric supports feature vectors.
         * Defaults to true.
         */
        public boolean supportsFeatureVectors = true;

        /**
         * Measure of similarity between vectors
         * Defaults to PairwiseCosineSimilarity.
         */
        public PairwiseSimilarity vectorSimilarity = new PairwiseCosineSimilarity();

        /**
         * Whether or not the metric should build a cosimilarity matrix
         * Deafaults to true.
         */
        public boolean buildCosimilarityMatrix = true;
    };

    /**
     * The base class uses this method to support its featureset.
     *
     * @return
     */
    public abstract MetricConfig getMetricConfig();

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

    public SRResultList getCachedMostSimilar(TIntFloatMap vector, int numResults, TIntSet validIds) throws DaoException {
        if (mostSimilarCache == null) {
            return null;
        }
        try {
            return mostSimilarCache.mostSimilar(vector, numResults, validIds);
        } catch (IOException e){
            throw new DaoException(e);
        }
    }

    public SRResultList getCachedMostSimilar(int wpId, int numResults, TIntSet validIds) throws DaoException {
        if (mostSimilarCache == null) {
            return null;
        }
        try {
            return mostSimilarCache.mostSimilar(wpId, numResults, validIds);
        } catch (IOException e){
            throw new DaoException(e);
        }
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
        MetricConfig  config = getMetricConfig();

        if (!dataDir.isDirectory()) {
            LOG.warning("directory " + dataDir + " does not exist; cannot read files");
            return;
        }
        if (shouldReadNormalizers && normalizers.hasReadableNormalizers(dataDir)) {
            normalizers.read(dataDir);
        }
        IOUtils.closeQuietly(mostSimilarCache);
        MostSimilarCache srm = new MostSimilarCache(this, config.vectorSimilarity, dataDir);
        if (srm.hasReadableMatrices()) {
            srm.read();
            mostSimilarCache = srm;
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
    }

    @Override
    public abstract SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException;

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        Language language = getLanguage();
        List<LocalString> phrases = Arrays.asList(
                new LocalString(language, phrase1),
                new LocalString(language, phrase2));
        List<LocalId> resolution = disambiguator.disambiguateTop(phrases, null);
        LocalId similar1 = resolution.get(0);
        LocalId similar2 = resolution.get(1);
        if (similar1==null||similar2==null){
            return new SRResult();
        }
        return similarity(similar1.getId(), similar2.getId(), explanations);
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
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
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

    @Override
    public void writeMostSimilarCache(int maxHits) throws IOException, DaoException, WikapidiaException {
        writeMostSimilarCache(maxHits, null, null);
    }

    @Override
    public void writeMostSimilarCache(int maxHits, TIntSet rowIds, TIntSet colIds) throws IOException, DaoException, WikapidiaException{
        MetricConfig  config = getMetricConfig();

        if (!config.buildCosimilarityMatrix && !config.supportsFeatureVectors) {
            IOUtils.closeQuietly(mostSimilarCache);
            mostSimilarCache = null;
            return;
        }

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

        MostSimilarCache srm = new MostSimilarCache(this, config.vectorSimilarity, dataDir);
        try {
            if (config.supportsFeatureVectors) {
                srm.writeFeatureAndTransposeMatrix(rowIds.toArray(), WpThreadUtils.getMaxThreads());
            }
            if (config.buildCosimilarityMatrix) {
                srm.writeCosimilarity(rowIds.toArray(), colIds.toArray(), maxHits, WpThreadUtils.getMaxThreads());
            }
            IOUtils.closeQuietly(mostSimilarCache);
            mostSimilarCache = srm;
        } catch (InterruptedException e){
            throw new RuntimeException(e);
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

    public MostSimilarCache getMostSimilarCache() {
        return mostSimilarCache;
    }

    protected static void configureBase(Configurator configurator, BaseMonolingualSRMetric sr, Config config) throws ConfigurationException {
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

        try {
            sr.read();
        } catch (IOException e){
            throw new ConfigurationException(e);
        }
    }
}
