package org.wikapidia.sr;

import com.typesafe.config.Config;
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
import org.wikapidia.sr.pairwise.PairwiseSimilarity;
import org.wikapidia.sr.pairwise.SRMatrices;
import org.wikapidia.sr.utils.SrNormalizers;
import org.wikapidia.utils.WpIOUtils;
import org.wikapidia.utils.WpThreadUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public abstract class BaseMonolingualSRMetric implements MonolingualSRMetric {
    private static Logger LOG = Logger.getLogger(BaseMonolingualSRMetric.class.getName());

    private Language language;
    private Disambiguator disambiguator;
    private LocalPageDao localPageDao;

    private boolean shouldReadNormalizers = true;
    private SrNormalizers normalizers;

    private SRMatrices mostSimilarMatrices = null;

    public BaseMonolingualSRMetric(Language language, LocalPageDao dao, Disambiguator disambig) {
        this.language = language;
        this.disambiguator = disambig;
        this.localPageDao = dao;
        this.normalizers =  new SrNormalizers();
    }

    public boolean hasCachedMostSimilarLocal(int wpId) {
        if (mostSimilarMatrices == null) {
            return false;
        }
        try {
            return mostSimilarMatrices.getCosimilarityMatrix().getRow(wpId) != null;
        } catch (IOException e) {
            throw new RuntimeException(e);  // should not happen
        }
    }

    public SRResultList getCachedMostSimilarLocal(int wpId, int numResults, TIntSet validIds) {
        if (!hasCachedMostSimilarLocal(wpId)){
            return null;
        }
        try {
            return mostSimilarMatrices.mostSimilar(wpId, numResults, validIds);
        } catch (IOException e){
            return null;
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
    public void write(String path) throws IOException {
        File dir = new File(path, getName());
        WpIOUtils.mkdirsQuietly(dir);
        normalizers.write(dir);
    }

    public void setReadNormalizers(boolean shouldRead) {
        this.shouldReadNormalizers = shouldRead;
    }

    @Override
    public void read(String path) throws IOException {
        File dir = new File(path, getName());
        if (!dir.isDirectory()) {
            LOG.warning("directory " + dir + " does not exist; cannot read files");
            return;
        }
        if (shouldReadNormalizers) {
            if (normalizers.hasReadableNormalizers(dir)) {
                normalizers.read(dir);
            }
        }
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
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
        HashSet<LocalString> context = new HashSet<LocalString>();
        context.add(new LocalString(language,phrase2));
        LocalId similar1 = disambiguator.disambiguate(new LocalString(language, phrase1), context);
        context.clear();
        context.add(new LocalString(language,phrase1));
        LocalId similar2 = disambiguator.disambiguate(new LocalString(language,phrase2),context);
        if (similar1==null||similar2==null){
            return new SRResult();
        }
        return similarity(similar1.getId(), similar2.getId(), explanations);
    }

    @Override
    public abstract SRResultList mostSimilar(int pageId, int maxResults) throws DaoException;

    @Override
    public abstract SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException;

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults) throws DaoException {
        LocalId similar = disambiguator.disambiguate(new LocalString(getLanguage(), phrase),null);
        if (similar==null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult());
            return resultList;
        }
        return mostSimilar(similar.getId(), maxResults);
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        LocalId similar = disambiguator.disambiguate(new LocalString(getLanguage(), phrase),null);
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
        List<LocalId> localIds = disambiguator.disambiguate(localStringList, null);
        for (int i=0; i<phrases.length; i++){
            ids[i] = localIds.get(i).getId();
        }
        return cosimilarity(ids);
    }

    @Override
    public void writeCosimilarity(String parentDir, int maxHits) throws IOException, DaoException, WikapidiaException {
        writeCosimilarity(parentDir, maxHits, null, null);
    }

    protected void writeCosimilarity(String parentDir, int maxHits, PairwiseSimilarity pairwise, TIntSet rowIds, TIntSet colIds) throws IOException, DaoException, WikapidiaException{
        try {
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

            File dir = FileUtils.getFile(parentDir, getName(), getLanguage().getLangCode());
            SRMatrices srm = new SRMatrices(this, pairwise, dir);
            srm.write(colIds.toArray(), rowIds.toArray(), maxHits, WpThreadUtils.getMaxThreads());
            mostSimilarMatrices = srm;
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    protected void readCosimilarity(String parentDir, PairwiseSimilarity pairwise) throws IOException {
        IOUtils.closeQuietly(mostSimilarMatrices);

        File dir = FileUtils.getFile(parentDir, getName(), getLanguage().getLangCode());
        SRMatrices srm = new SRMatrices(this, pairwise, dir);
        if (srm.hasReadableMatrices()) {
            srm.readMatrices();
            mostSimilarMatrices = srm;
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

    public boolean isShouldReadNormalizers() {
        return shouldReadNormalizers;
    }

    public SrNormalizers getNormalizers() {
        return normalizers;
    }

    @Override
    public Normalizer getMostSimilarNormalizer() {
        return normalizers.getMostSimilarNormalizer();
    }

    @Override
    public Normalizer getSimilarityNormalizer() {
        return normalizers.getSimilarityNormalizer();
    }

    public SRMatrices getMostSimilarMatrices() {
        return mostSimilarMatrices;
    }

    protected static void configureBase(Configurator configurator, BaseMonolingualSRMetric sr, Config config) throws ConfigurationException {
        Config rootConfig = configurator.getConf().get();
        File path = new File(rootConfig.getString("sr.metric.path"));
        boolean isTraining = rootConfig.getBoolean("sr.metric.training");

        // initialize normalizers
        sr.setSimilarityNormalizer(configurator.get(Normalizer.class, config.getString("similaritynormalizer")));
        sr.setMostSimilarNormalizer(configurator.get(Normalizer.class, config.getString("mostsimilarnormalizer")));

        if (isTraining) {
            sr.setReadNormalizers(false);
        }

        try {
            sr.read(path.getAbsolutePath());
        } catch (IOException e){
            throw new ConfigurationException(e);
        }

        try {
            sr.readCosimilarity(path.getAbsolutePath());
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }
}
