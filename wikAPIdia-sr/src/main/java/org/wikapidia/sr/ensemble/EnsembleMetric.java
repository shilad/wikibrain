package org.wikapidia.sr.ensemble;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wikapidia.conf.Configuration;
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
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.matrix.SparseMatrixWriter;
import org.wikapidia.matrix.ValueConf;
import org.wikapidia.sr.*;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.utils.*;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Matt Lesicko
 * @author Shilad Sen
 */
public class EnsembleMetric extends BaseMonolingualSRMetric {
    private static final Logger LOG = Logger.getLogger(EnsembleMetric.class.getName());

    private final int EXTRA_SEARCH_DEPTH = 2;
    private double missingScores[];
    private int missingRanks[];
    private List<MonolingualSRMetric> metrics;
    Ensemble ensemble;

    private SparseMatrix mostSimilarMatrices = null;

    public EnsembleMetric(Language language, List<MonolingualSRMetric> metrics, Ensemble ensemble, Disambiguator disambiguator, LocalPageDao pageHelper){
        super(language, pageHelper, disambiguator);
        this.metrics=metrics;
        this.ensemble=ensemble;
        this.missingScores = new double[metrics.size()];
        this.missingRanks = new int[metrics.size()];
        Arrays.fill(missingScores, 0);
        Arrays.fill(missingRanks, 1000);
    }

    @Override
    public String getName() {
        return "ensemble";
    }

    public List<MonolingualSRMetric> getMetrics() {
        return metrics;
    }

    @Override
    public boolean hasCachedMostSimilarLocal(int wpId){
        if (mostSimilarMatrices == null) {
            return false;
        }
        try {
            return mostSimilarMatrices.getRow(wpId)!=null;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public SRResultList getCachedMostSimilarLocal(int wpId, int numResults, TIntSet validIds){
        if (!hasCachedMostSimilarLocal(wpId)){
            return null;
        }
        SparseMatrixRow row;
        try{
            row = mostSimilarMatrices.getRow(wpId);
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

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (MonolingualSRMetric metric : metrics){
            scores.add(metric.similarity(pageId1,pageId2,explanations));
            }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (MonolingualSRMetric metric : metrics){
            scores.add(metric.similarity(phrase1,phrase2,explanations));
        }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
        return mostSimilar(pageId,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarLocal(pageId)){
            SRResultList mostSimilar= getCachedMostSimilarLocal(pageId, maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            List<SRResultList> scores = new ArrayList<SRResultList>();
            for (MonolingualSRMetric metric : metrics){
                scores.add(metric.mostSimilar(pageId,maxResults*EXTRA_SEARCH_DEPTH,validIds));
            }
            return ensemble.predictMostSimilar(scores,maxResults);
        }
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults) throws  DaoException{
        return mostSimilar(phrase, maxResults, null);
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        List<SRResultList> scores = new ArrayList<SRResultList>();
        for (MonolingualSRMetric metric : metrics){
            scores.add(metric.mostSimilar(phrase,maxResults*EXTRA_SEARCH_DEPTH,validIds));
        }
        return ensemble.predictMostSimilar(scores,maxResults);
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        for (MonolingualSRMetric metric : metrics) {
            metric.trainSimilarity(dataset);
        }
        List<EnsembleSim> ensembleSims = new ArrayList<EnsembleSim>();

        for (KnownSim ks : dataset.getData()){
            EnsembleSim es = new EnsembleSim(ks);
            for (MonolingualSRMetric metric : metrics){
                double score = Double.NaN;
                try {
                    score = metric.similarity(ks.phrase1,ks.phrase2,false).getScore();
                } catch (Exception e){
                    LOG.log(Level.WARNING, "Local sr metric " + metric.getName() + " failed for " + ks, e);
                }
                es.add(score, 0);
            }
            ensembleSims.add(es);
        }
        estimateInterpolatedValues(ensembleSims);
        interpolateValues(ensembleSims);
        ensemble.trainSimilarity(ensembleSims);
    }

    @Override
    public void trainMostSimilar(Dataset dataset, final int numResults, final TIntSet validIds){
        mostSimilarMatrices = null;
        for (MonolingualSRMetric metric : metrics){
            metric.trainMostSimilar(dataset,numResults,validIds);
        }
        List<EnsembleSim> ensembleSims = ParallelForEach.loop(dataset.getData(), new Function<KnownSim,EnsembleSim>() {
            public EnsembleSim call(KnownSim ks) throws DaoException{
                List<LocalString> localStrings = Arrays.asList(
                        new LocalString(ks.language, ks.phrase1),
                        new LocalString(ks.language, ks.phrase2)
                );
                List<LocalId> ids = getDisambiguator().disambiguate(localStrings, null);
                if (ids.isEmpty() || ids.get(0).getId() <= 0) {
                    return null;
                }
                int pageId = ids.get(0).getId();
                EnsembleSim es = new EnsembleSim(ks);
                for (MonolingualSRMetric metric : metrics) {
                    double score = Double.NaN;
                    int rank = -1;
                    try {
                        SRResultList dsl = metric.mostSimilar(pageId, numResults*EXTRA_SEARCH_DEPTH, validIds);
                        if (dsl!=null&&dsl.getIndexForId(ids.get(1).getId())>1){
                            score = dsl.getScore(dsl.getIndexForId(ids.get(1).getId()));
                            rank = dsl.getIndexForId(ids.get(1).getId());
                        }
                    } catch (Exception e){
                        LOG.log(Level.WARNING, "Local sr metric " + metric.getName() + " failed for " + pageId, e);
                    } finally {
                        es.add(score, rank);
                    }
                }
                return es;
            }
        },100);
        estimateInterpolatedValues(ensembleSims);
        interpolateValues(ensembleSims);
        ensemble.trainMostSimilar(ensembleSims);
    }

    /**
     * calculate interpolated values for missing ranks and scores
     * @param examples
     */
    private void estimateInterpolatedValues(List<EnsembleSim> examples) {
        for (int i = 0; i < metrics.size(); i++) {
            int maxMissingRanks = -1;
            int numMissingScores = 0;
            double sumMissingScores = 0.0;
            for (EnsembleSim es : examples) {
                double v = es.getScores().get(i);
                if (Double.isNaN(v) || Double.isInfinite(v)) {
                    sumMissingScores += es.getKnownSim().similarity;
                    numMissingScores++;
                }
                maxMissingRanks = Math.max(maxMissingRanks, es.getRanks().get(i));
            }
            missingRanks[i] = Math.max(100, maxMissingRanks * 2);
            missingScores[i] = numMissingScores > 0 ? (sumMissingScores / numMissingScores) : 0.0;
            LOG.info("for metric " + metrics.get(i).getName() + ", " +
                    " estimated missing rank " + missingRanks[i] +
                    " and missing score " + missingScores[i]);
        }
    }

    private void interpolateValues(List<EnsembleSim> examples) {
        for (int i = 0; i < metrics.size(); i++) {
            for (EnsembleSim es : examples) {
                double v = es.getScores().get(i);
                if (Double.isNaN(v) || Double.isInfinite(v)) {
                    es.getScores().set(i, missingScores[i]);
                }
                if (es.getRanks().get(i) < 0) {
                    es.getRanks().set(i, missingRanks[i]);
                }
            }
        }
    }

    @Override
    public void write(String path) throws  IOException {
        File dir = new File(path);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        ensemble.write(new File(dir, "ensemble").getAbsolutePath());
        FileUtils.writeByteArrayToFile(new File(dir, "missingScores"), WpIOUtils.objectToBytes(missingScores));
        FileUtils.writeByteArrayToFile(new File(dir, "missingRanks"), WpIOUtils.objectToBytes(missingRanks));
    }

    @Override
    public void read(String path) throws IOException{
        File dir = new File(path);
        try {
            missingScores = (double[]) WpIOUtils.bytesToObject(
                    FileUtils.readFileToByteArray(new File(dir, "missingScores")));
            missingRanks = (int[]) WpIOUtils.bytesToObject(
                    FileUtils.readFileToByteArray(new File(dir, "missingRanks")));
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        ensemble.read(new File(dir, "ensemble").getAbsolutePath());
    }

    @Override
    public TIntDoubleMap getVector(int id) throws DaoException {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path, int maxHits) throws IOException, DaoException, WikapidiaException {
        DaoFilter pageFilter = new DaoFilter()
            .setLanguages(getLanguage())
                .setNameSpaces(NameSpace.ARTICLE)
                .setDisambig(false)
                .setRedirect(false);
        Iterable<LocalPage> localPages = getLocalPageDao().get(pageFilter);
        TIntSet pageIds = new TIntHashSet();
        for (LocalPage page: localPages){
            if (page != null){
                pageIds.add(page.getLocalId());
            }
        }
        List<Integer> idList = new ArrayList<Integer>();
        for (int id : pageIds.toArray()){
            idList.add(id);
        }
        File file = FileUtils.getFile(path, getName(), getLanguage().getLangCode());
        if (!file.getParentFile().isDirectory()) {
            file.getParentFile().mkdirs();
        }
        writeMatrix(idList,file,maxHits);
        readCosimilarity(path);
    }



     private void writeMatrix(List<Integer> rowIds, File path, final int maxHits) throws IOException, WikapidiaException {
        ValueConf vconf = new ValueConf();
        final SparseMatrixWriter writer = new SparseMatrixWriter(path, vconf);
        ParallelForEach.loop(rowIds, WpThreadUtils.getMaxThreads(), new Procedure<Integer>() {
            @Override
            public void call(Integer id) throws Exception {
                writeVector(writer, id, maxHits);
            }
        }, 10000);
        try {
            writer.finish();
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
    }

    private void writeVector(SparseMatrixWriter writer,int id,int maxHits) throws WikapidiaException {
        SRResultList scores;
        try {
            scores = mostSimilar(id,maxHits);
        } catch (DaoException e){
            throw new WikapidiaException(e);
        }
        LinkedHashMap<Integer,Float> linkedHashMap = new LinkedHashMap<Integer, Float>();
        for (SRResult score : scores){
            linkedHashMap.put(score.getId(),(float)score.getScore());
        }
        try {
            writer.writeRow(new SparseMatrixRow(writer.getValueConf(), id, linkedHashMap));
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
    }

    @Override
    public void readCosimilarity(String dir) throws IOException {
        IOUtils.closeQuietly(mostSimilarMatrices);
        mostSimilarMatrices = null;
        File file = FileUtils.getFile(dir, getName(), getLanguage().getLangCode());
        SparseMatrix matrix = new SparseMatrix(file);
        if (matrix!=null){
            mostSimilarMatrices = matrix;
        }
    }

    public static class Provider extends org.wikapidia.conf.Provider<MonolingualSRMetric>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return MonolingualSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public MonolingualSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException{
            if (!config.getString("type").equals("ensemble")) {
                return null;
            }
            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            if (!config.hasPath("metrics")){
                throw new ConfigurationException("Ensemble metric has no base metrics to use.");
            }
            EnsembleMetric sr;
            List<MonolingualSRMetric> metrics = new ArrayList<MonolingualSRMetric>();
            for (String metric : config.getStringList("metrics")){
                metrics.add(getConfigurator().get(MonolingualSRMetric.class, metric, "language", language.getLangCode()));
            }
            Ensemble ensemble;
            if (config.getString("ensemble").equals("linear")){
                ensemble = new LinearEnsemble(metrics.size());
            } else if (config.getString("ensemble").equals("even")){
                ensemble = new EvenEnsemble();
            } else {
                throw new ConfigurationException("I don't know how to do that ensemble.");
            }
            Disambiguator disambiguator = getConfigurator().get(Disambiguator.class,config.getString("disambiguator"));
            LocalPageDao pagehelper = getConfigurator().get(LocalPageDao.class,config.getString("pageDao"));
            sr = new EnsembleMetric(language, metrics,ensemble,disambiguator,pagehelper);

            try {
                sr.read(getConfigurator().getConf().get().getString("sr.metric.path")+sr.getName());
            } catch (IOException e){
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            sr.setSimilarityNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")));
            sr.setMostSimilarNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")));
            try {
                sr.readCosimilarity(getConfigurator().getConf().get().getString("sr.metric.path"));
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

//            try {
//                sr.readCosimilarity(getConfigurator().getConf().get().getString("sr.metric.path"),langs);
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//                e.printStackTrace();
//            }

            return sr;
        }
    }
}
