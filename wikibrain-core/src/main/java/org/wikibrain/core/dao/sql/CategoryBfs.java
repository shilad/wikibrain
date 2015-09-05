package org.wikibrain.core.dao.sql;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.CategoryGraph;
import org.wikibrain.core.model.NameSpace;

import java.util.Collection;
import java.util.PriorityQueue;

/**
 * Conducts Dijkstra on the category hierarchy from a starting document.
 * Pages attached to visited categories are recorded, and iterations stop
 * when a certain number of unique pages have been discovered.

 * @author Shilad Sen
 * @author Matt Lesicko
 */
public class CategoryBfs {
    private CategoryGraph graph;
    private int startPage;
    private int maxResults;
    private LocalCategoryMemberDao categoryMemberDao;
    private Language language;

    /**
     * Observed distances to visited categories.
     */
    private TIntDoubleHashMap catDistances = new TIntDoubleHashMap();

    /**
     * Observed distances to visited pages.
     */
    private TIntDoubleHashMap pageDistances = new TIntDoubleHashMap();

    /**
     * Categories that have been seen, but not visited.
     */
    private PriorityQueue<CategoryDistance> openCats = new PriorityQueue<CategoryDistance>();

    /**
     * Results of the current iteration.
     */
    private BfsVisited visited = new BfsVisited();

    /**
     * If true, tracks pages visited along the way.
     */
    public boolean addPages = true;

    /**
     * If true, explore paths that travel up to an ancestor and back down to a descendant.
     * If false, only travel upwards.
     */
    public boolean exploreChildren = true;

    /**
     * Wikipedia ids that can be traversed in the result set.
     */
    private TIntSet validWpIds;

    int numSteps = 0;

    public CategoryBfs(CategoryGraph graph, int startCatId, Language language, int maxResults, TIntSet validWpIds, LocalCategoryMemberDao categoryMemberDao) throws DaoException {
        this(graph, startCatId, NameSpace.ARTICLE, language, maxResults, validWpIds, categoryMemberDao, (byte)+1);
    }

    public CategoryBfs(CategoryGraph graph, int startId, NameSpace startNamespace, Language language, int maxResults, TIntSet validWpIds, LocalCategoryMemberDao categoryMemberDao, int direction) throws DaoException {
        this.startPage = startId;
        this.maxResults = maxResults;
        this.graph = graph;
        this.validWpIds = validWpIds;
        this.categoryMemberDao = categoryMemberDao;
        this.language = language;
        pageDistances.put(startPage, 0.000000);
        if (startNamespace == NameSpace.ARTICLE) {
            Collection<Integer> cats = categoryMemberDao.getCategoryIds(language, startId);
            if (cats!=null){
                for (int catId : cats) {
                    int ci = graph.catIdToIndex(catId);
                    if (ci >= 0) {
                        openCats.add(new CategoryDistance(ci, graph.cats[ci], graph.catCosts[ci], (byte)direction));
                    }
                }
            }
        } else if (startNamespace == NameSpace.CATEGORY) {
            int ci = graph.catIdToIndex(startId);
            if (ci >= 0) {
                openCats.add(new CategoryDistance(ci, graph.cats[ci], 0.000000001, (byte)direction));
            }
        } else {
            throw new IllegalArgumentException();
        }

    }

    public void setAddPages(boolean addPages) {
        this.addPages = addPages;
    }

    public void setExploreChildren(boolean exploreChildren) {
        this.exploreChildren = exploreChildren;
    }

    public boolean hasMoreResults() {
        return openCats.size() > 0 && pageDistances.size() < maxResults;
    }

    /**
     * Runs one step of Dijjkstra by visiting the closest unvisited category.
     * @return A BfsVisited object that captures all pages and categories visited in the step.
     */
    public BfsVisited step() {
        numSteps++;

        visited.clear();
        if (!hasMoreResults()) {
            return visited;
        }
        CategoryDistance cs;
        do {
            cs = openCats.poll();
        } while (hasMoreResults() && catDistances.contains(cs.getCatIndex()));

        visited.cats.put(cs.getCatIndex(), cs.getDistance());
        catDistances.put(cs.getCatIndex(), cs.getDistance());
//        System.out.println("visited " + cs.toString());

        // add directly linked pages
        if (addPages) {
            for (int i : graph.catPages[cs.getCatIndex()]) {
                if (validWpIds != null && !validWpIds.contains(i)) {
                    continue;
                }
                if (!pageDistances.containsKey(i) || pageDistances.get(i) > cs.getDistance()) {
                    pageDistances.put(i, cs.getDistance());
                    visited.pages.put(i, cs.getDistance());
                }
                if (pageDistances.size() >= maxResults) {
                    break;  // may be an issue for huge categories
                }
            }
        }

        // next steps downwards
        if (exploreChildren) {
            for (int i : graph.catChildren[cs.getCatIndex()]) {
                if (!catDistances.containsKey(i)) {
                    double d = cs.getDistance() + graph.catCosts[i];
                    openCats.add(new CategoryDistance(i, graph.cats[i], d, (byte)-1));
                }
            }
        }

        // next steps upwards (if still possible)
        if (cs.getDirection() == +1) {
            for (int i : graph.catParents[cs.getCatIndex()]) {
                if (!catDistances.containsKey(i)) {
                    double d = cs.getDistance() + graph.catCosts[i];
                    openCats.add(new CategoryDistance(i, graph.cats[i], d, (byte)+1));
                }
            }
        }

        return visited;
    }

    public TIntDoubleHashMap getPageDistances() {
        return pageDistances;
    }
    public boolean hasPageDistance(int pageId) {
        return pageDistances.containsKey(pageId);
    }
    public double getPageDistance(int pageId) {
        return pageDistances.get(pageId);
    }
    public boolean hasCategoryDistanceForIndex(int categoryId) {
        return catDistances.containsKey(categoryId);
    }
    public boolean hasCategoryDistance(int pageId) {
        return catDistances.containsKey(graph.catIdToIndex(pageId));
    }
    public double getCategoryDistance(int categoryId) {
        return catDistances.get(graph.catIdToIndex(categoryId));
    }
    public double getCategoryDistanceForIndex(int catIndex) {
        return catDistances.get(catIndex);
    }

    public class BfsVisited {
        public TIntDoubleHashMap pages = new TIntDoubleHashMap();
        public TIntDoubleHashMap cats = new TIntDoubleHashMap();
        public void clear() { pages.clear(); cats.clear(); }
        public double maxPageDistance() { return max(pages.values()); }
        public double maxCatDistance() { return max(cats.values()); }
    }

    private double max(double []A) {
        double max = Double.NEGATIVE_INFINITY;
        for (double x : A) {
            if (x > max) max = x;
        }
        return max;
    }
}
