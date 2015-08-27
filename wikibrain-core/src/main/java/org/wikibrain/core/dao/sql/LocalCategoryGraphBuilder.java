package org.wikibrain.core.dao.sql;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.*;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a directed graph among categories and pages using daos.
 * Also calculates page rank among pages.
 */
public class LocalCategoryGraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCategoryGraphBuilder.class);

    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catDao;

    public LocalCategoryGraphBuilder(LocalPageDao pageDao, LocalCategoryMemberDao catDao) {
        this.pageDao = pageDao;
        this.catDao = catDao;
    }

    /**
     *
     * @param language
     * @return
     * @throws DaoException
     */
    public CategoryGraph build(Language language) throws DaoException {
        CategoryGraph graph = new CategoryGraph(language);
        loadCategories(graph);
        buildGraph(graph);
        computePageRanks(graph);
        return graph;
    }

    private void loadCategories(CategoryGraph graph) throws DaoException {
        LOG.info("loading categories...");
        graph.catIndexes = new TIntIntHashMap();
        List<String> catList = new ArrayList<String>();
        Iterable<LocalPage> catIter = pageDao.get(new DaoFilter()
                .setNameSpaces(NameSpace.CATEGORY)
                .setLanguages(graph.language)
        );
        TIntList catIds = new TIntArrayList();
        for (LocalPage cat : catIter) {
            if (cat != null) {
                if (graph.catIndexes.containsKey(cat.getLocalId())) {
                    continue;
                }
                assert(catList.size() == graph.catIndexes.size());
                assert(catIds.size() == graph.catIndexes.size());
                int ci = graph.catIndexes.size();
                graph.catIndexes.put (cat.getLocalId(), ci);
                catList.add(cat.getTitle().getCanonicalTitle());
                catIds.add(cat.getLocalId());
            }
        }
        graph.cats = catList.toArray(new String[0]);
        graph.catIds = catIds.toArray();
        LOG.info("finished loading " + graph.cats.length + " categories");
    }

    private void buildGraph(CategoryGraph graph) throws DaoException {
        LOG.info("building category graph");
        graph.catPages = new int[graph.catIndexes.size()][];
        graph.catParents = new int[graph.catIndexes.size()][];
        graph.catChildren = new int[graph.catIndexes.size()][];
        graph.catCosts = new double[graph.catIndexes.size()];

        Arrays.fill(graph.catPages, new int[0]);
        Arrays.fill(graph.catParents, new int[0]);
        Arrays.fill(graph.catChildren, new int[0]);

        // count reverse edges
        int totalEdges = 0;
        int numCatChildren[] = new int[graph.catIndexes.size()];
        int numCatParents[] = new int[graph.catIndexes.size()];
        int numCatPages[] = new int[graph.catIndexes.size()];

        DaoFilter filter = new DaoFilter().setLanguages(graph.language);
        for (LocalCategoryMember lcm : catDao.get(filter)) {
            int catIndex1 = graph.catIdToIndex(lcm.getArticleId());     // cat index for page (probably -1)
            int catIndex2 = graph.catIdToIndex(lcm.getCategoryId());    // cat index for cat
            if (catIndex1 >= 0 && catIndex2 >= 0) {
                numCatChildren[catIndex2]++;
                numCatParents[catIndex1]++;
            } else if (catIndex2 >= 0) {
                numCatPages[catIndex2]++;
            }
            totalEdges++;
        }

        // allocate space
        for (int i = 0; i < graph.catIndexes.size(); i++) {
            graph.catPages[i] = new int[numCatPages[i]];
            graph.catChildren[i] = new int[numCatChildren[i]];
            graph.catParents[i] = new int[numCatParents[i]];
        }

        // fill it
        for (LocalCategoryMember lcm : catDao.get(filter)) {
            int catIndex1 = graph.catIdToIndex(lcm.getArticleId());     // cat index for page (probably -1)
            int catIndex2 = graph.catIdToIndex(lcm.getCategoryId());    // cat index for cat
            if (catIndex1 >= 0 && catIndex2 >= 0) {
                graph.catChildren[catIndex2][--numCatChildren[catIndex2]] = catIndex1;
                graph.catParents[catIndex1][--numCatParents[catIndex1]] = catIndex2;
            } else if (catIndex2 >= 0) {
                graph.catPages[catIndex2][--numCatPages[catIndex2]] = lcm.getArticleId();
            }
        }

        for (int n : numCatChildren) { assert(n == 0); }
        for (int n : numCatPages) { assert(n == 0); }
        for (int n : numCatParents) { assert(n == 0); }
        LOG.info("loaded " + totalEdges + " edges in category graph");
    }

    public void computePageRanks(CategoryGraph graph) {
        if (graph.catIds.length == 0) {
            LOG.info("No categories found. Skipping page rank calculation.");
            return;
        }
        LOG.info("computing category page ranks...");

        // initialize page rank
        long sumCredits = graph.catPages.length;    // each category gets 1 credit to start
        for (int i = 0; i < graph.catPages.length; i++) {
            sumCredits += graph.catPages[i].length; // one more credit per page that references it.
        }
        for (int i = 0; i < graph.catPages.length; i++) {
            graph.catCosts[i] = (1.0 + graph.catPages[i].length) / sumCredits;
        }

        for (int i = 0; i < 20; i++) {
            LOG.info("performing page ranks iteration {0}.", i);
            double error = onePageRankIteration(graph);
            LOG.info("Error for iteration is {0}.", error);
            if (error == 0) {
                break;
            }
        }
        Integer sortedIndexes[] = new Integer[graph.catCosts.length];
        for (int i = 0; i < graph.catParents.length; i++) {
            graph.catCosts[i] = 1.0/-Math.log(graph.catCosts[i]);
            sortedIndexes[i] = i;
        }
        LOG.info("finished computing page ranks...");
        final double[] costs = graph.catCosts;
        Arrays.sort(sortedIndexes, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                Double pr1 = costs[i1];
                Double pr2 = costs[i2];
                return -1 * pr1.compareTo(pr2);
            }
        });

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 20 && i < sortedIndexes.length; i++) {
            int j = sortedIndexes[i];
            b.append("" + i + ". " + graph.cats[j] + "=" + graph.catCosts[j]);
            b.append(", ");
        }
        graph.minCost = graph.catCosts[sortedIndexes[sortedIndexes.length - 1]];

        LOG.info("Min cat cost: " + graph.minCost);
        LOG.info("Top cat costs: " + b.toString());
    }

    private static final double DAMPING_FACTOR = 0.85;
    public double onePageRankIteration(CategoryGraph graph) {
        double nextRanks [] = new double[graph.catCosts.length];
        Arrays.fill(nextRanks, (1.0 - DAMPING_FACTOR) / graph.catCosts.length);
        for (int i = 0; i < graph.catParents.length; i++) {
            int d = graph.catParents[i].length;   // degree
            double pr = graph.catCosts[i];    // current page-rank
            for (int j : graph.catParents[i]) {
                nextRanks[j] += DAMPING_FACTOR * pr / d;
            }
        }
        double diff = 0.0;
        for (int i = 0; i < graph.catParents.length; i++) {
            diff += Math.abs(graph.catCosts[i] - nextRanks[i]);
        }
        graph.catCosts = nextRanks;
        return diff;
    }

}
