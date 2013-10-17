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
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.pairwise.PairwiseSimilarity;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.pairwise.SRMatrices;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.sr.utils.SrNormalizers;
import org.wikapidia.utils.WpIOUtils;
import org.wikapidia.utils.WpThreadUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public abstract class BaseLocalSRMetric implements LocalSRMetric {
    private static Logger LOG = Logger.getLogger(BaseLocalSRMetric.class.getName());
    protected Disambiguator disambiguator;
    protected LocalPageDao pageHelper;


    protected SrNormalizers defaultNormalizers = new SrNormalizers();
    protected Map<Language, SrNormalizers> normalizers = new HashMap<Language, SrNormalizers>();

    protected Map<Language,SRMatrices> mostSimilarMatrices = new HashMap<Language, SRMatrices>();
    private boolean shouldReadNormalizers = true;

    public boolean hasCachedMostSimilarLocal(Language language, int wpId) {
        if (!mostSimilarMatrices.containsKey(language)) {
            return false;
        }
        try {
            return mostSimilarMatrices.get(language).getCosimilarityMatrix().getRow(wpId) != null;
        } catch (IOException e) {
            throw new RuntimeException(e);  // should not happen
        }
    }

    public SRResultList getCachedMostSimilarLocal(Language language, int wpId, int numResults, TIntSet validIds) {
        if (!hasCachedMostSimilarLocal(language, wpId)){
            return null;
        }
        SparseMatrixRow row;
        try {
            row = mostSimilarMatrices.get(language).getCosimilarityMatrix().getRow(wpId);
        } catch (IOException e){
            return null;
        }
        Leaderboard leaderboard = new Leaderboard(numResults);
        for (int i=0; i<row.getNumCols() ; i++){
            int wpId2 = row.getColIndex(i);
            float value = row.getColValue(i);
            if (validIds == null || validIds.contains(wpId2)){
                leaderboard.tallyScore(wpId2,value);
            }
        }
        SRResultList results = leaderboard.getTop();
        results.sortDescending();
        return results;
    }

    /**
     * Normalizers translate similarity scores to more meaningful values.
     * @param n
     */
    @Override
    public void setDefaultMostSimilarNormalizer(Normalizer n){
        this.defaultNormalizers.setMostSimilarNormalizer(n);
    }

    @Override
    public void setDefaultSimilarityNormalizer(Normalizer defaultSimilarityNormalizer) {
        this.defaultNormalizers.setSimilarityNormalizer(defaultSimilarityNormalizer);
    }

    @Override
    public void setMostSimilarNormalizer(Normalizer n, Language l){
        if (!normalizers.containsKey(l)) {
            normalizers.put(l, new SrNormalizers());
        }
        normalizers.get(l).setMostSimilarNormalizer(n);
    }

    @Override
    public void setSimilarityNormalizer(Normalizer n, Language l){
        if (!normalizers.containsKey(l)) {
            normalizers.put(l, new SrNormalizers());
        }
        normalizers.get(l).setSimilarityNormalizer(n);
    }

    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureSimilarityTrained() {
        if (!defaultNormalizers.getSimilarityNormalizer().isTrained()) {
            throw new IllegalStateException("Model default similarity has not been trained.");
        }
    }
    /**
     * Throws an IllegalStateException if the model has not been mostSimilarTrained.
     */
    protected void ensureMostSimilarTrained() {
        if (!defaultNormalizers.getMostSimilarNormalizer().isTrained()) {
            throw new IllegalStateException("Model default mostSimilar has not been trained.");
        }
    }

    /**
     * Use the language-specific similarity normalizer to normalize a similarity if it exists.
     * Otherwise use the default similarity normalizer if it's available.
     * @param sr
     * @param language
     * @return
     */
    protected SRResult normalize(SRResult sr, Language language) {
        sr.score=normalize(sr.score,language);
        return sr;
    }

    /**
     * Use the language-specific most similar normalizer to normalize a similarity if it exists.
     * Otherwise use the default most similar normalizer if it's available.
     * @param srl
     * @param l
     * @return
     */
    protected SRResultList normalize(SRResultList srl, Language l) {
        if (normalizers.containsKey(l) &&
            normalizers.get(l).getMostSimilarNormalizer().isTrained()) {
            return normalizers.get(l).getMostSimilarNormalizer().normalize(srl);
        }
        ensureMostSimilarTrained();
        return defaultNormalizers.getMostSimilarNormalizer().normalize(srl);
    }

    protected double normalize (double score, Language l){
        if (normalizers.containsKey(l) &&
                normalizers.get(l).getSimilarityNormalizer().isTrained()) {
            return normalizers.get(l).getSimilarityNormalizer().normalize(score);
        }
        ensureSimilarityTrained();
        return defaultNormalizers.getSimilarityNormalizer().normalize(score);
    }

    @Override
    public void write(String path) throws IOException {
        File dir = new File(path, getName());
        WpIOUtils.mkdirsQuietly(dir);
        defaultNormalizers.write(dir);
        for (Language l : normalizers.keySet()) {
            File langDir = new File(dir, l.getLangCode());
            WpIOUtils.mkdirsQuietly(langDir);
            normalizers.get(l).write(langDir);
        }
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
            if (defaultNormalizers.hasReadableNormalizers(dir)) {
                defaultNormalizers.read(dir);
            }
            for (File f : dir.listFiles()) {
                if (f.isDirectory() && Language.hasLangCode(f.getName())) {
                    Language l = Language.getByLangCode(f.getName());
                    SrNormalizers np = normalizers.get(l);
                    if (np == null) np = new SrNormalizers();
                    if (np.hasReadableNormalizers(f)) {
                        np.read(f);
                    }
                    normalizers.put(l, np);
                }
            }
        }
    }

    @Override
    public void trainDefaultSimilarity(Dataset dataset){
        defaultNormalizers.trainSimilarity(this, dataset);
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        if (!normalizers.containsKey(dataset.language)) {
            normalizers.put(dataset.getLanguage(), new SrNormalizers());
        }
        normalizers.get(dataset.getLanguage()).trainSimilarity(this, dataset);
    }

    @Override
    public synchronized void trainDefaultMostSimilar(Dataset dataset, int numResults, TIntSet validIds){
        defaultNormalizers.trainMostSimilar(this, disambiguator, dataset, validIds, numResults);
    }

    @Override
    public synchronized void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds){
        if (!normalizers.containsKey(dataset.language)) {
            normalizers.put(dataset.getLanguage(), new SrNormalizers());
        }
        normalizers.get(dataset.getLanguage())
                .trainMostSimilar(this, disambiguator, dataset, validIds, numResults);
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
        if (similar1==null||similar2==null){
            return new SRResult();
        }
        return similarity(pageHelper.getById(language,similar1.getId()),
                pageHelper.getById(language,similar2.getId()),
                explanations);
    }

    @Override
    public abstract SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException;

    @Override
    public abstract SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException;

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws DaoException {
        LocalId similar = disambiguator.disambiguate(phrase,null);
        return mostSimilar(pageHelper.getById(similar.getLanguage(),similar.getId()), maxResults);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException {
        LocalId similar = disambiguator.disambiguate(phrase,null);
        if (similar==null){
            SRResultList resultList = new SRResultList(1);
            resultList.set(0, new SRResult());
            return resultList;
        }
        return mostSimilar(pageHelper.getById(similar.getLanguage(),similar.getId()), maxResults,validIds);
    }

    @Override
    public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds, Language language) throws DaoException {
        double[][] cos = new double[wpRowIds.length][wpColIds.length];
        for (int i=0; i<wpRowIds.length; i++){
            for (int j=0; j<wpColIds.length; j++){
                if (wpRowIds[i]==wpColIds[j]){
                    cos[i][j]=normalize(1.0,language);
                }
                else{
                    cos[i][j]=similarity(
                            new LocalPage(language,wpRowIds[i],null,null),
                            new LocalPage(language,wpColIds[j],null,null),
                            false).getScore();
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
                    cos[i][j]=normalize(1.0,language);
                }
                else{
                    cos[i][j]=similarity(rowPhrases[i],colPhrases[j],language, false).getScore();
                }
            }
        }
        return cos;
    }

    @Override
    public double[][] cosimilarity(int[] ids, Language language) throws DaoException {
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=normalize(1.0,language);
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]=similarity(
                        new LocalPage(language, ids[i], null, null),
                        new LocalPage(language, ids[j], null, null),
                        false).getScore();
                cos[j][i]=cos[i][j];
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

    protected void writeCosimilarity(String parentDir, LanguageSet languages, int maxHits, PairwiseSimilarity pairwise) throws IOException, DaoException, WikapidiaException{
        try {
            for (Language language: languages) {
                // Get all page ids
                DaoFilter pageFilter = new DaoFilter()
                        .setLanguages(language)
                        .setNameSpaces(NameSpace.ARTICLE) // TODO: should this come from conf?
                        .setDisambig(false)
                        .setRedirect(false);
                Iterable<LocalPage> localPages = pageHelper.get(pageFilter);
                TIntSet pageIds = new TIntHashSet();
                for (LocalPage page : localPages) {
                    if (page != null) {
                        pageIds.add(page.getLocalId());
                    }
                }

                File dir = FileUtils.getFile(parentDir, getName(), language.getLangCode());
                SRMatrices srm = new SRMatrices(this, language, pairwise, dir);
                srm.write(pageIds.toArray(), null, WpThreadUtils.getMaxThreads());
            }
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    protected void readCosimilarity(String parentDir, LanguageSet languages, PairwiseSimilarity pairwise) throws IOException {
        for (SRMatrices srm : mostSimilarMatrices.values()) {
            IOUtils.closeQuietly(srm);
        }
        mostSimilarMatrices.clear();

        for (Language language: languages) {
            File dir = FileUtils.getFile(parentDir, getName(), language.getLangCode());
            SRMatrices srm = new SRMatrices(this, language, pairwise, dir);
            if (srm.hasReadableMatrices()) {
                srm.readMatrices();
                mostSimilarMatrices.put(language, srm);
            }
        }
    }

    protected static void configureBase(Configurator configurator, BaseLocalSRMetric sr, Config config) throws ConfigurationException {
        Config rootConfig = configurator.getConf().get();
        File path = new File(rootConfig.getString("sr.metric.path"));
        boolean isTraining = rootConfig.getBoolean("sr.metric.training");
        LanguageSet languages = configurator.get(LanguageSet.class);

        // initialize normalizers
        sr.setDefaultSimilarityNormalizer(configurator.get(Normalizer.class, config.getString("similaritynormalizer")));
        sr.setDefaultMostSimilarNormalizer(configurator.get(Normalizer.class, config.getString("mostsimilarnormalizer")));


        for (Language language: languages){
            sr.setSimilarityNormalizer(configurator.get(Normalizer.class, config.getString("similaritynormalizer")), language);
            sr.setMostSimilarNormalizer(configurator.get(Normalizer.class, config.getString("mostsimilarnormalizer")), language);
        }
        if (isTraining) {
            sr.setReadNormalizers(false);
        }

        try {
            sr.read(path.getAbsolutePath());
        } catch (IOException e){
            throw new ConfigurationException(e);
        }

        try {
            sr.readCosimilarity(path.getAbsolutePath(), languages);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }
}
