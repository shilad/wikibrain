//package org.wikibrain.sr.category;
//
//import com.typesafe.config.Config;
//import gnu.trove.map.TIntDoubleMap;
//import gnu.trove.map.hash.TIntDoubleHashMap;
//import gnu.trove.set.TIntSet;
//import org.wikibrain.conf.Configuration;
//import org.wikibrain.conf.ConfigurationException;
//import org.wikibrain.conf.Configurator;
//import org.wikibrain.core.WikiBrainException;
//import org.wikibrain.core.dao.DaoException;
//import org.wikibrain.core.dao.LocalCategoryMemberDao;
//import org.wikibrain.core.dao.LocalPageDao;
//import org.wikibrain.core.lang.Language;
//import org.wikibrain.core.lang.LanguageSet;
//import org.wikibrain.core.model.LocalPage;
//import org.wikibrain.sr.BaseLocalSRMetric;
//import org.wikibrain.sr.LocalSRMetric;
//import org.wikibrain.sr.SRResult;
//import org.wikibrain.sr.SRResultList;
//import org.wikibrain.sr.disambig.Disambiguator;
//import org.wikibrain.sr.utils.SimUtils;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Map;
//
///**
// * @author Matt Lesicko
// */
//public class CategorySimilarity extends BaseLocalSRMetric {
//    private LocalCategoryMemberDao categoryMemberDao;
//
//    public CategorySimilarity(Disambiguator disambiguator, LocalCategoryMemberDao categoryMemberDao,LocalPageDao pageHelper){
//        this.disambiguator = disambiguator;
//        this.pageHelper = pageHelper;
//        this.categoryMemberDao = categoryMemberDao;
//    }
//
//    @Override
//    public String getName() {
//        return "categorysimilarity";
//    }
//
//    @Override
//    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
//        TIntDoubleMap v1 = getVector(page1.getLocalId(),page1.getLanguage());
//        TIntDoubleMap v2 = getVector(page2.getLocalId(),page2.getLanguage());
//        return new SRResult(SimUtils.cosineSimilarity(v1,v2));
//    }
//
//    @Override
//    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
//        Collection<Integer> cats = categoryMemberDao.getCategoryIds(language, id);
//        TIntDoubleMap vector = new TIntDoubleHashMap();
//        if (cats!=null){
//            ArrayList<Integer> extraCats = new ArrayList<Integer>();
//            while (!cats.isEmpty()){
//                Integer cat = cats.iterator().next();
//                //Get parent categories
//                Collection<Integer> parentCats = categoryMemberDao.getCategoryIds(language,cat);
//                if (parentCats!=null){
//                    for (Integer i: parentCats){
//                        if (!vector.containsKey(i)&&!extraCats.contains(i)){
//                            extraCats.add(i);
//                        }
//                    }
//                }
//                cats.remove(cat);
//                vector.put(cat,1);
//            }
//            while (!extraCats.isEmpty()){
//                Integer cat = extraCats.iterator().next();
//                //Get parent categories
//                Collection<Integer> parentCats = categoryMemberDao.getCategoryIds(language,cat);
//                if (parentCats!=null){
//                    for (Integer i: parentCats){
//                        if (!vector.containsKey(i)&&!extraCats.contains(i)){
//                            extraCats.add(i);
//                        }
//                    }
//                }
//                extraCats.remove(cat);
//                vector.put(cat,.1);
//            }
//        }
//        return vector;
//    }
//
//    @Override
//    public void writeMostSimilarCache(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikiBrainException {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void readCosimilarity(String path, LanguageSet languages) throws IOException {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    public static class Provider extends org.wikibrain.conf.Provider<LocalSRMetric> {
//        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
//            super(configurator, config);
//        }
//
//        @Override
//        public Class getType() {
//            return LocalSRMetric.class;
//        }
//
//        @Override
//        public String getPath() {
//            return "sr.metric.local";
//        }
//
//        @Override
//        public LocalSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
//            if (!config.getString("type").equals("categorysimilarity")) {
//                return null;
//            }
//
//            CategorySimilarity sr = new CategorySimilarity(
//                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
//                    getConfigurator().get(LocalCategoryMemberDao.class,config.getString("categoryMemberDao")),
//                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao"))
//            );
//            configureBase(getConfigurator(), sr, config);
//            return sr;
//        }
//
//    }
//}
