package org.wikapidia.sr.ensemble;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.Configuration;
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
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.matrix.SparseMatrixWriter;
import org.wikapidia.matrix.ValueConf;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.utils.Function;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;
import org.wikapidia.utils.WpThreadUtils;

import java.io.*;
import java.util.*;

/**
 * @author Matt Lesicko
 */
public class EnsembleMetric extends BaseLocalSRMetric{
    private final int EXTRA_SEARCH_DEPTH = 2;
    private final double missingScore = 0.0;
    private final int missingRank = 100;
    private List<LocalSRMetric> metrics;
    Ensemble ensemble;

    private Map<Language,SparseMatrix> mostSimilarMatrices = new HashMap<Language, SparseMatrix>();

    public EnsembleMetric(List<LocalSRMetric> metrics, Ensemble ensemble, Disambiguator disambiguator, LocalPageDao pageHelper){
        this.metrics=metrics;
        this.ensemble=ensemble;
        this.disambiguator=disambiguator;
        this.pageHelper=pageHelper;
    }

    @Override
    public String getName() {
        return "ensemble";
    }

    @Override
    public boolean hasCachedMostSimilarLocal (Language language, int wpId){
        if (!mostSimilarMatrices.containsKey(language)){
            return false;
        }
        try {
            return mostSimilarMatrices.get(language).getRow(wpId)!=null;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public SRResultList getCachedMostSimilarLocal(Language language, int wpId, int numResults, TIntSet validIds){
        if (!hasCachedMostSimilarLocal(language,wpId)){
            return null;
        }
        SparseMatrixRow row;
        try{
            row = mostSimilarMatrices.get(language).getRow(wpId);
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
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (LocalSRMetric metric : metrics){
            scores.add(metric.similarity(page1,page2,explanations));
        }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (LocalSRMetric metric : metrics){
            scores.add(metric.similarity(phrase1,phrase2,language,explanations));
        }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
        return mostSimilar(page,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarLocal(page.getLanguage(), page.getLocalId())){
            SRResultList mostSimilar= getCachedMostSimilarLocal(page.getLanguage(), page.getLocalId(), maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            List<SRResultList> scores = new ArrayList<SRResultList>();
            for (LocalSRMetric metric : metrics){
                scores.add(metric.mostSimilar(page,maxResults*EXTRA_SEARCH_DEPTH,validIds));
            }
            return ensemble.predictMostSimilar(scores,maxResults);
        }
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws  DaoException{
        return mostSimilar(phrase,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException {
        List<SRResultList> scores = new ArrayList<SRResultList>();
        for (LocalSRMetric metric : metrics){
            scores.add(metric.mostSimilar(phrase,maxResults*EXTRA_SEARCH_DEPTH,validIds));
        }
        return ensemble.predictMostSimilar(scores,maxResults);
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        for (LocalSRMetric metric : metrics) {
            metric.trainSimilarity(dataset);
        }
        List<EnsembleSim> ensembleSims = new ArrayList<EnsembleSim>();
        for (KnownSim ks : dataset.getData()){
            List<Double> scores = new ArrayList<Double>();
            List<Integer> ranks = new ArrayList<Integer>();
            for (LocalSRMetric metric : metrics){
                double score;
                try {
                    score = metric.similarity(ks.phrase1,ks.phrase2,ks.language,false).getScore();
                }
                catch (DaoException e){
                    score = Double.NaN;
                }
                if (!Double.isNaN(score)&&!Double.isInfinite(score)){
                    scores.add(score);
                } else {
                    scores.add(missingScore);
                }
                ranks.add(0); //Don't worry about ranks when training similarity
            }
            ensembleSims.add(new EnsembleSim(scores, ranks, ks));
        }
        ensemble.trainSimilarity(ensembleSims);
    }

    @Override
    public void trainDefaultSimilarity(Dataset dataset) throws DaoException {
        for (LocalSRMetric metric : metrics) {
            metric.trainDefaultSimilarity(dataset);
        }
        List<EnsembleSim> ensembleSims = new ArrayList<EnsembleSim>();
        for (KnownSim ks : dataset.getData()){
            List<Double> scores = new ArrayList<Double>();
            List<Integer> ranks = new ArrayList<Integer>();
            for (LocalSRMetric metric : metrics){
                try {
                    scores.add(metric.similarity(ks.phrase1,ks.phrase2,ks.language,false).getScore());
                }
                catch (DaoException e){
                    scores.add(missingScore);
                }
                ranks.add(0); //Don't worry about ranks when training similarity
            }
            ensembleSims.add(new EnsembleSim(scores,ranks,ks));
        }
        ensemble.trainSimilarity(ensembleSims);
    }

    @Override
    public void trainMostSimilar(Dataset dataset, final int numResults, final TIntSet validIds){
        mostSimilarMatrices.clear();
        for (LocalSRMetric metric : metrics){
            metric.trainMostSimilar(dataset,numResults,validIds);
        }
        List<EnsembleSim> ensembleSims = ParallelForEach.loop(dataset.getData(), new Function<KnownSim,EnsembleSim>() {
            public EnsembleSim call(KnownSim ks) throws DaoException{
                List<Double> scores = new ArrayList<Double>();
                List<Integer> ranks = new ArrayList<Integer>();
                List<LocalString> localStrings = new ArrayList<LocalString>();
                localStrings.add(new LocalString(ks.language, ks.phrase1));
                localStrings.add(new LocalString(ks.language, ks.phrase2));
                List<LocalId> ids = disambiguator.disambiguate(localStrings, null);
                LocalPage page = pageHelper.getById(ks.language,ids.get(0).getId());
                if (page!=null){
                    for (LocalSRMetric metric : metrics){
                        try {
                            SRResultList dsl = metric.mostSimilar(page, numResults*EXTRA_SEARCH_DEPTH, validIds);
                            if (dsl!=null&&dsl.getIndexForId(ids.get(1).getId())>1){
                                scores.add(dsl.getScore(dsl.getIndexForId(ids.get(1).getId())));
                                ranks.add(dsl.getIndexForId(ids.get(1).getId()));
                            }
                            else {
                                scores.add(missingScore);
                                ranks.add(missingRank);
                            }
                        } catch (DaoException e){
                            scores.add(missingScore);
                            ranks.add(missingRank);
                        } //In event of metric failure
                    }
                    return new EnsembleSim(scores,ranks,ks);
                }
                return null;
            }
        },1);
        ensemble.trainMostSimilar(ensembleSims);
    }

    @Override
    public void write(String path) throws  IOException{
        ensemble.write(path);
    }

    @Override
    public void read(String path) throws IOException{
        ensemble.read(path);
    }

    @Override
    public void trainDefaultMostSimilar(Dataset dataset, int numResults, TIntSet validIds){
        //TODO: implement me
    }

    @Override
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException {
        for (Language language : languages){
            DaoFilter pageFilter = new DaoFilter()
                    .setLanguages(language)
                    .setNameSpaces(NameSpace.ARTICLE)
                    .setDisambig(false)
                    .setRedirect(false);
            Iterable<LocalPage> localPages = pageHelper.get(pageFilter);
            TIntSet pageIds = new TIntHashSet();
            for (LocalPage page: localPages){
                if (page != null){
                    pageIds.add(page.getLocalId());
                }
            }
            File dir = FileUtils.getFile(path,getName(),language.getLangCode());
            List<Integer> idList = new ArrayList<Integer>();
            for (int id : pageIds.toArray()){
                idList.add(id);
            }
            writeMatrix(idList,dir,language, maxHits);
        }
        readCosimilarity(path,languages);
    }



     private void writeMatrix(List<Integer> rowIds, File dir, final Language language, final int maxHits) throws IOException, WikapidiaException {
        ValueConf vconf = new ValueConf();
        final SparseMatrixWriter writer = new SparseMatrixWriter(dir,vconf);
        ParallelForEach.loop(rowIds, WpThreadUtils.getMaxThreads(), new Procedure<Integer>() {
            @Override
            public void call(Integer id) throws Exception {
                writeVector(writer, id, language, maxHits);
            }
        }, 10000);
        try {
            writer.finish();
        } catch (IOException e){
            throw new WikapidiaException(e);
        }
    }

    private void writeVector(SparseMatrixWriter writer,int id,Language language, int maxHits) throws WikapidiaException {
        SRResultList scores;
        try {
            LocalPage page = pageHelper.getById(language,id);
            scores = mostSimilar(page,maxHits);
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
    public void readCosimilarity(String path, LanguageSet languages) throws IOException {
        mostSimilarMatrices.clear();
        for (Language language: languages) {
            File dir = FileUtils.getFile(path,getName(),language.getLangCode());
            SparseMatrix matrix = new SparseMatrix(dir);
            if (matrix!=null){
                mostSimilarMatrices.put(language,matrix);
            }
        }
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalSRMetric>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public LocalSRMetric get(String name, Config config) throws ConfigurationException{
            if (!config.getString("type").equals("ensemble")) {
                return null;
            }

            LanguageSet langs = getConfigurator().get(LanguageSet.class);

            if (config.hasPath("metrics")){
                EnsembleMetric sr;
                List<LocalSRMetric> metrics = new ArrayList<LocalSRMetric>();
                for (String metric : config.getStringList("metrics")){
                    metrics.add(getConfigurator().get(LocalSRMetric.class,metric));
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
                sr = new EnsembleMetric(metrics,ensemble,disambiguator,pagehelper);

                try {
                    sr.read(getConfigurator().getConf().get().getString("sr.metric.path")+sr.getName());
                } catch (IOException e){
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }


                //Set up normalizers
                sr.setDefaultSimilarityNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                sr.setDefaultMostSimilarNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                for (Language language : langs){
                    sr.setSimilarityNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                    sr.setMostSimilarNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                }

                try {
                    sr.readCosimilarity(getConfigurator().getConf().get().getString("sr.metric.path"),langs);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }

                return sr;
            }
            else {
                throw new ConfigurationException("Ensemble metric has no base metrics to use.");
            }
        }
    }
}
