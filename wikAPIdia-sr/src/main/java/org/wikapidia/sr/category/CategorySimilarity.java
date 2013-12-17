package org.wikapidia.sr.category;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.BaseLocalSRMetric;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.SimUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author Matt Lesicko
 */
public class CategorySimilarity extends BaseLocalSRMetric {
    private LocalCategoryMemberDao categoryMemberDao;

    public CategorySimilarity(Disambiguator disambiguator, LocalCategoryMemberDao categoryMemberDao,LocalPageDao pageHelper){
        this.disambiguator = disambiguator;
        this.pageHelper = pageHelper;
        this.categoryMemberDao = categoryMemberDao;
    }

    @Override
    public String getName() {
        return "categorysimilarity";
    }

    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        TIntDoubleMap v1 = getVector(page1.getLocalId(),page1.getLanguage());
        TIntDoubleMap v2 = getVector(page2.getLocalId(),page2.getLanguage());
        return new SRResult(SimUtils.cosineSimilarity(v1,v2));
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        Collection<Integer> cats = categoryMemberDao.getCategoryIds(language, id);
        TIntDoubleMap vector = new TIntDoubleHashMap();
        if (cats!=null){
            ArrayList<Integer> extraCats = new ArrayList<Integer>();
            while (!cats.isEmpty()){
                Integer cat = cats.iterator().next();
                //Get parent categories
                Collection<Integer> parentCats = categoryMemberDao.getCategoryIds(language,cat);
                if (parentCats!=null){
                    for (Integer i: parentCats){
                        if (!vector.containsKey(i)&&!extraCats.contains(i)){
                            extraCats.add(i);
                        }
                    }
                }
                cats.remove(cat);
                vector.put(cat,1);
            }
            while (!extraCats.isEmpty()){
                Integer cat = extraCats.iterator().next();
                //Get parent categories
                Collection<Integer> parentCats = categoryMemberDao.getCategoryIds(language,cat);
                if (parentCats!=null){
                    for (Integer i: parentCats){
                        if (!vector.containsKey(i)&&!extraCats.contains(i)){
                            extraCats.add(i);
                        }
                    }
                }
                extraCats.remove(cat);
                vector.put(cat,.1);
            }
        }
        return vector;
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void readCosimilarity(String path, LanguageSet languages) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalSRMetric> {
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
        public LocalSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("categorysimilarity")) {
                return null;
            }

            CategorySimilarity sr = new CategorySimilarity(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalCategoryMemberDao.class,config.getString("categoryMemberDao")),
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao"))
            );
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
