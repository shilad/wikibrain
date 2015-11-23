package org.wikibrain.sr.category;

import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.sql.CategoryBfs;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.CategoryGraph;
import org.wikibrain.sr.*;
import org.wikibrain.sr.BaseSRMetric;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;

import java.util.ArrayList;
import java.util.Map;

/**
 * <p>This metric is an enhanced variant of Stube and Ponzetto's WikiRelate.</p>
 *
 * <p>This class makes two enhancements to the original SR metric that improve
 * the accuracy and efficiency of the algorithm calculations. First, this class
 * uses page-rank weighted edge weights for the category graph to distinguish
 * between categories with different levels of specificity. Second, this class
 * uses bidirectional search (instead of vanilla breadth-first search) for the
 * similarity() method.</p>
 *
 * @author Matt Lesicko
 * @author Shilad Sen
 */
public class CategoryGraphSimilarity extends BaseSRMetric {
    private static final Logger LOG = LoggerFactory.getLogger(CategoryGraphSimilarity.class);

    private final CategoryGraph graph;
    LocalCategoryMemberDao catHelper;

    public CategoryGraphSimilarity(String name, Language language, LocalPageDao pageDao, Disambiguator disambiguator, LocalCategoryMemberDao categoryMemberDao) throws DaoException {
        super(name, language,pageDao,disambiguator);
        this.catHelper=categoryMemberDao;
        this.graph = categoryMemberDao.getGraph(language);
    }

    public double distanceToScore(double distance) {
        return distanceToScore(graph, distance);
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
    public SRConfig getConfig() {
        SRConfig config = new SRConfig();
        config.minScore = -1.0f;
        config.maxScore = +1.0f;
        return config;
    }

    /**
     * Some languages do not have categories. Don't choke on them!
     * @param dataset
     * @throws DaoException
     */
    @Override
    public synchronized void trainSimilarity(Dataset dataset) throws DaoException {
        try {
            super.trainSimilarity(dataset);
        } catch (Exception e) {
            LOG.warn("Training of sr metric similarity " + getName() + " failed, disabling it.", e);
        }
    }

    /**
     * Some languages do not have categories. Don't choke on them!
     */
    @Override
    public synchronized void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
        try {
            super.trainMostSimilar(dataset, numResults, validIds);
        } catch (Exception e) {
            LOG.warn("Training of sr metric mostSimilar " + getName() + " failed, disabling it.", e);
        }
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        if (!similarityIsTrained()) {
            return new SRResult(0.0);
        }
        CategoryBfs bfs1 = new CategoryBfs(graph,pageId1,getLanguage(), Integer.MAX_VALUE, null, catHelper);
        CategoryBfs bfs2 = new CategoryBfs(graph,pageId2,getLanguage(), Integer.MAX_VALUE, null, catHelper);
        bfs1.setAddPages(false);
        bfs1.setExploreChildren(false);
        bfs2.setAddPages(false);
        bfs2.setExploreChildren(false);

        double shortestDistance = Double.POSITIVE_INFINITY;
        double maxDist1 = 0;
        double maxDist2 = 0;

        // Note that all the category ids below are dense indexes in [0, numCategories).
        // The mapping is determined by the graph.
        while ((bfs1.hasMoreResults() || bfs2.hasMoreResults())
                &&     (maxDist1 + maxDist2 < shortestDistance)) {
            // Search from d1
            while (bfs1.hasMoreResults() && (maxDist1 <= maxDist2 || !bfs2.hasMoreResults())) {
                CategoryBfs.BfsVisited visited = bfs1.step();
                for (int catId : visited.cats.keys()) {
                    if (bfs2.hasCategoryDistanceForIndex(catId)) {
                        double d = bfs1.getCategoryDistanceForIndex(catId)
                                + bfs2.getCategoryDistanceForIndex(catId)
                                - graph.catCosts[catId];    // counted twice
                        shortestDistance = Math.min(d, shortestDistance);
                    }
                }
                maxDist1 = Math.max(maxDist1, visited.maxCatDistance());
            }

            // Search from d2
            while (bfs2.hasMoreResults() && (maxDist2 <= maxDist1 || !bfs1.hasMoreResults())) {
                CategoryBfs.BfsVisited visited = bfs2.step();
                for (int catId : visited.cats.keys()) {
                    if (bfs1.hasCategoryDistanceForIndex(catId)) {
                        double d = bfs1.getCategoryDistanceForIndex(catId) +
                                bfs2.getCategoryDistanceForIndex(catId) + 0
                                - graph.catCosts[catId];    // counted twice;
                        shortestDistance = Math.min(d, shortestDistance);
                    }
                }
                maxDist2 = Math.max(maxDist2, visited.maxCatDistance());
            }
        }

        return new SRResult(distanceToScore(shortestDistance));
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        if (!mostSimilarIsTrained()) {
            return new SRResultList(0);
        }

        SRResultList results = getCachedMostSimilar(pageId, maxResults, validIds);
        if (results != null) {
            return results;
        }
        CategoryBfs bfs = new CategoryBfs(graph,pageId,getLanguage(), maxResults, validIds, catHelper);
        while (bfs.hasMoreResults()) {
            bfs.step();
        }
        results = new SRResultList(bfs.getPageDistances().size());
        int i = 0;
        for (int pageId2: bfs.getPageDistances().keys()) {
            results.set(i++, pageId2, distanceToScore(bfs.getPageDistances().get(pageId2)));
        }
        results.sortDescending();
        return normalize(results);
    }

    public static class Provider extends org.wikibrain.conf.Provider<SRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("categorygraphsimilarity")) {
                return null;
            }

            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("LocalCategoryGraphBuilder requires 'language' runtime parameter.");
            }

            Language language = Language.getByLangCode(runtimeParams.get("language"));

            CategoryGraphSimilarity sr = null;
            try {
                sr = new CategoryGraphSimilarity(
                        name,
                        language,
                        getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                        getConfigurator().get(Disambiguator.class,config.getString("disambiguator"), "language", language.getLangCode()),
                        getConfigurator().get(LocalCategoryMemberDao.class,config.getString("categoryMemberDao"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
