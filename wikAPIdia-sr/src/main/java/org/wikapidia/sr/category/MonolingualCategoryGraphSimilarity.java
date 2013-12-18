package org.wikapidia.sr.category;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.BaseMonolingualSRMetric;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.disambig.Disambiguator;

import java.io.IOException;
import java.util.Map;

/**
 * @author Matt Lesicko
 * @author Shilad Sen
 */
public class MonolingualCategoryGraphSimilarity extends BaseMonolingualSRMetric{
    MonolingualCategoryGraphHelper graphHelper;
    LocalCategoryMemberDao catHelper;

    public MonolingualCategoryGraphSimilarity(Language language, LocalPageDao pageDao, Disambiguator disambiguator, LocalCategoryMemberDao categoryMemberDao, MonolingualCategoryGraphHelper graphHelper) {
        super(language,pageDao,disambiguator);
        this.catHelper=categoryMemberDao;
        this.graphHelper = graphHelper;
        try {
            this.graphHelper.init();
        } catch (DaoException e) {
            //TODO: handle this error
        }

    }

    @Override
    public String getName() {
        return "categorygraphsimilarity";
    }

    public double distanceToScore(double distance) {
        return distanceToScore(graphHelper.graph(), distance);
    }

    public static double distanceToScore(CategoryGraph graph, double distance) {
        distance = Math.max(distance, graph.minCost);
        assert(graph.minCost < 1.0);    // if this isn't true, direction is flipped.
        if (Double.isInfinite(distance)){
            return 0.0;
        }
        return  (Math.log(distance) / Math.log(graph.minCost));
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        CategoryBfs bfs1 = new CategoryBfs(graphHelper.graph(),pageId1,getLanguage(), Integer.MAX_VALUE, null, catHelper);
        CategoryBfs bfs2 = new CategoryBfs(graphHelper.graph(),pageId2,getLanguage(), Integer.MAX_VALUE, null, catHelper);
        bfs1.setAddPages(false);
        bfs1.setExploreChildren(false);
        bfs2.setAddPages(false);
        bfs2.setExploreChildren(false);

        double shortestDistance = Double.POSITIVE_INFINITY;
        double maxDist1 = 0;
        double maxDist2 = 0;

        while ((bfs1.hasMoreResults() || bfs2.hasMoreResults())
                &&     (maxDist1 + maxDist2 < shortestDistance)) {
            // Search from d1
            while (bfs1.hasMoreResults() && (maxDist1 <= maxDist2 || !bfs2.hasMoreResults())) {
                CategoryBfs.BfsVisited visited = bfs1.step();
                for (int catId : visited.cats.keys()) {
                    if (bfs2.hasCategoryDistance(catId)) {
                        double d = bfs1.getCategoryDistance(catId)
                                + bfs2.getCategoryDistance(catId)
                                - graphHelper.graph().catCosts[catId];    // counted twice
                        shortestDistance = Math.min(d, shortestDistance);
                    }
                }
                maxDist1 = Math.max(maxDist1, visited.maxCatDistance());
            }

            // Search from d2
            while (bfs2.hasMoreResults() && (maxDist2 <= maxDist1 || !bfs1.hasMoreResults())) {
                CategoryBfs.BfsVisited visited = bfs2.step();
                for (int catId : visited.cats.keys()) {
                    if (bfs1.hasCategoryDistance(catId)) {
                        double d = bfs1.getCategoryDistance(catId) +
                                bfs2.getCategoryDistance(catId) + 0
                                - graphHelper.graph().catCosts[catId];    // counted twice;
                        shortestDistance = Math.min(d, shortestDistance);
                    }
                }
                maxDist2 = Math.max(maxDist2, visited.maxCatDistance());
            }
        }

        return new SRResult(distanceToScore(shortestDistance));
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
        return mostSimilar(pageId, maxResults, null);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarLocal(pageId)) {
            return getCachedMostSimilarLocal(pageId, maxResults, validIds);
        }
        CategoryBfs bfs = new CategoryBfs(graphHelper.graph(),pageId,getLanguage(), maxResults, validIds, catHelper);
        while (bfs.hasMoreResults()) {
            bfs.step();
        }
        SRResultList results = new SRResultList(bfs.getPageDistances().size());
        int i = 0;
        for (int pageId2: bfs.getPageDistances().keys()) {
            results.set(i++, pageId2, distanceToScore(bfs.getPageDistances().get(pageId2)));
        }
        return normalize(results);
    }

    @Override
    public TIntDoubleMap getVector(int id) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path,int maxHits) throws IOException, DaoException, WikapidiaException {
        //TODO: implement me
    }

    @Override
    public void readCosimilarity(String path) throws IOException {
        //TODO: implement me
    }

    public static class Provider extends org.wikapidia.conf.Provider<MonolingualSRMetric> {
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
        public MonolingualSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("categorygraphsimilarity")) {
                return null;
            }

            if (!runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("MonolingualCategoryGraphHelper requires 'language' runtime parameter.");
            }

            Language language = Language.getByLangCode(runtimeParams.get("language"));

            MonolingualCategoryGraphSimilarity sr = new MonolingualCategoryGraphSimilarity(
                    language,
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalCategoryMemberDao.class,config.getString("categoryMemberDao")),
                    getConfigurator().get(MonolingualCategoryGraphHelper.class, "categorygraphsimilarity", "language", language.getLangCode())
            );
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
