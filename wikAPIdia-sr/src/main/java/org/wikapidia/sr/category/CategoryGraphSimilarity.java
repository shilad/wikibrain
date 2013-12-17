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
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.BaseLocalSRMetric;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.disambig.Disambiguator;

import java.io.IOException;
import java.util.Map;

/**
 * @author Matt Lesicko
 */
public class CategoryGraphSimilarity extends BaseLocalSRMetric{
    CategoryGraphHelper graphHelper;
    LocalCategoryMemberDao catHelper;

    public CategoryGraphSimilarity (Disambiguator disambiguator, LocalCategoryMemberDao categoryMemberDao, LocalPageDao pageDao, CategoryGraphHelper graphHelper) {
        this.disambiguator=disambiguator;
        this.catHelper=categoryMemberDao;
        this.pageHelper=pageDao;
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

    public double distanceToScore(double distance, Language lang) {
        return distanceToScore(graphHelper.graph(lang), distance);
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
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        Language lang = page1.getLanguage();
        CategoryBfs bfs1 = new CategoryBfs(graphHelper.graph(lang),page1.getLocalId(),page1.getLanguage(), Integer.MAX_VALUE, null, catHelper);
        CategoryBfs bfs2 = new CategoryBfs(graphHelper.graph(lang),page2.getLocalId(),page2.getLanguage(), Integer.MAX_VALUE, null, catHelper);
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
                                - graphHelper.graph(lang).catCosts[catId];    // counted twice
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
                                - graphHelper.graph(lang).catCosts[catId];    // counted twice;
                        shortestDistance = Math.min(d, shortestDistance);
                    }
                }
                maxDist2 = Math.max(maxDist2, visited.maxCatDistance());
            }
        }

        return new SRResult(distanceToScore(shortestDistance, lang));
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException {
        //TODO: implement me
    }

    @Override
    public void readCosimilarity(String path, LanguageSet languages) throws IOException {
        //TODO: implement me
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
            if (!config.getString("type").equals("categorygraphsimilarity")) {
                return null;
            }

            CategoryGraphSimilarity sr = new CategoryGraphSimilarity(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalCategoryMemberDao.class,config.getString("categoryMemberDao")),
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(CategoryGraphHelper.class)
            );
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
