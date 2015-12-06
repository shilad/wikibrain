package org.wikibrain.core.dao.sql;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.*;

import java.io.File;
import java.util.*;

/**
 *
 * A SQL database implementation of the LocalCategoryMemberDao.
 *
 * @author Shilad Sen
 * @author Ari Weiland
 *
 */
public class LocalCategoryMemberSqlDao extends AbstractSqlDao<LocalCategoryMember> implements LocalCategoryMemberDao {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.CATEGORY_MEMBERS.LANG_ID,
            Tables.CATEGORY_MEMBERS.CATEGORY_ID,
            Tables.CATEGORY_MEMBERS.ARTICLE_ID,
    };
    private final LocalPageDao localPageDao;
    private Map<Language, CategoryGraph> graphs = new HashMap<Language, CategoryGraph>();

    /**
     * Only used to identify top-level categories.
     */
    private final UniversalPageDao univDao;

    // See https://www.wikidata.org/wiki/Q4587687
    public static final int TOP_LEVEL_CONCEPT = 4587687;

    // Language-specific top-level concept overrides (language -> title)
    private Map<Language, Title> topLevelLangOverrides = new HashMap<Language, Title>();

    public LocalCategoryMemberSqlDao(WpDataSource dataSource, LocalPageDao localArticleDao) throws DaoException {
        this(dataSource, localArticleDao, null);
    }


    public LocalCategoryMemberSqlDao(WpDataSource dataSource, LocalPageDao localArticleDao, UniversalPageDao univDao) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/category-members");
        this.localPageDao = localArticleDao;
        this.univDao = univDao;
    }

    @Override
    public void save(LocalCategoryMember member) throws DaoException {
        insert(
                member.getLanguage().getId(),
                member.getCategoryId(),
                member.getArticleId()
        );
    }

    public void addTopLevelOverride(Language language, String topLevelTitle) {
        this.topLevelLangOverrides.put(language, new Title(topLevelTitle, language));
    }

    @Override
    public Set<LocalPage> guessTopLevelCategories(Language language) throws DaoException {
        int topLevelId = -1;
        Title override = topLevelLangOverrides.get(language);
        if (override != null) {
            System.out.println("title is " + override);
            topLevelId = localPageDao.getIdByTitle(override);
            if (topLevelId < 0) {
                LOG.warn("top level category {} for language {} not found.", override, language);
            }
        }
        if (topLevelId < 0) {
            if (univDao == null) {
                throw new DaoException("Universal dao required for top level categories.");
            }
            topLevelId = univDao.getLocalId(language, TOP_LEVEL_CONCEPT);
        }
        Set<LocalPage> result = new HashSet<LocalPage>();
        if (topLevelId < 0) {
            return result;
        }
        for (int id : getCategoryMemberIds(language, topLevelId)) {
            LocalPage page = localPageDao.getById(language, id);
            if (page.getNameSpace() == NameSpace.CATEGORY) {
                result.add(page);
            }
        }

        return result;
    }

    @Override
    public void save(LocalPage category, LocalPage article) throws DaoException, WikiBrainException {
        if (!graphs.isEmpty()) {
            graphs.clear();
        }
        save(new LocalCategoryMember(category, article));
    }

    @Override
    public LocalPage getClosestCategory(LocalPage page, Set<LocalPage> candidates, boolean weightedDistance) throws DaoException {
        CategoryGraph graph = getGraph(page.getLanguage());
        CategoryBfs bfs = new CategoryBfs(graph, page.getLocalId(), page.getLanguage(), Integer.MAX_VALUE, null, this);
        bfs.setAddPages(false);
        bfs.setExploreChildren(false);
        Map<Integer, LocalPage> indexToCandidates = new HashMap<Integer, LocalPage>();
        for (LocalPage c : candidates) {
            indexToCandidates.put(graph.catIdToIndex(c.getLocalId()), c);
        }

        List<LocalPage> matches = new ArrayList<LocalPage>();
        while (bfs.hasMoreResults() && matches.isEmpty()) {
            CategoryBfs.BfsVisited visited = bfs.step();
            for (int catId : visited.cats.keys()) {
                if (indexToCandidates.containsKey(catId)) {
                    matches.add(indexToCandidates.get(catId));
                }
            }
        }
        if (matches.isEmpty()) {
            return null;
        } else {
            return matches.get(new Random().nextInt(matches.size()));
        }
    }

    static class CatCost implements Comparable<CatCost> {
        LocalPage topLevelCat;
        int catId;
        int catIndex;   // dense internal id from category graph, not an article id.
        double cost;

        public CatCost(LocalPage topLevelCat, int catId, int catIndex, double cost) {
            this.topLevelCat = topLevelCat;
            this.catId = catId;
            this.catIndex = catIndex;
            this.cost = cost;
        }

        @Override
        public int compareTo(CatCost o) {
            return Double.compare(cost, o.cost);
        }
    }



    @Override
    public Map<LocalPage, TIntDoubleMap> getClosestCategories(Set<LocalPage> topLevelCats) throws DaoException {
        return getClosestCategories(topLevelCats, null, true);
    }

    /**
     * For each article, identifies the closest category among the specified candidate set.
     * Distance is measured using shortest path in the category graph.
     *
     * @param candidateCategories   The categories to consider as candidates (e.g. those considered "top-level").
     * @param pageIds               If not null, only considers articles in the provided pageIds.
     * @param weighted              If true, use page-rank weighted edges so paths that traverse more
     *                              general categories are penalized more highly.
     * @return                      Map with candidates as keys and the articles that have them as closest category
     *                              as values. The values are a map of article ids to distances.
     * @throws DaoException
     */
    @Override
    public Map<LocalPage, TIntDoubleMap> getClosestCategories(Set<LocalPage> candidateCategories, TIntSet pageIds, boolean weighted) throws DaoException {
        Map<LocalPage, TIntDoubleMap> results = new HashMap<LocalPage, TIntDoubleMap>();
        if (candidateCategories.isEmpty()) {
            return results;
        }

        Language language = candidateCategories.iterator().next().getLanguage();
        CategoryGraph graph = getGraph(language);
        int numPages = (pageIds == null) ? LanguageInfo.getByLanguage(language).getNumArticles() : pageIds.size();

        PriorityQueue<CatCost> frontier = new PriorityQueue<CatCost>();
        for (LocalPage p : candidateCategories) {
            if (p.getLanguage() != language) throw new IllegalStateException("Category languages must be identitical");
            CatCost cc = new CatCost(p, p.getLocalId(), graph.catIdToIndex(p.getLocalId()), 0.0);
            if (cc.catIndex >= 0) {
                frontier.add(cc);
            }
            results.put(p, new TIntDoubleHashMap(numPages));
        }

        TIntSet visited = new TIntHashSet(numPages*3);   // both articles and categories
        while (!frontier.isEmpty()) {
            CatCost cc = frontier.poll();
            if (visited.contains(cc.catId)) continue;
            visited.add(cc.catId);

            // Handle pages of categories
            for (int pageId : graph.catPages[cc.catIndex]) {
                if (!visited.contains(pageId)
                &&  (pageIds == null || pageIds.contains(pageId))) {
                    visited.add(pageId);
                    results.get(cc.topLevelCat).put(pageId, cc.cost);
                }
            }

            // Descend to unexplored child categories.
            for (int childIndex : graph.catChildren[cc.catIndex]) {
                int childId = graph.catIndexToId(childIndex);
                if (!visited.contains(childId)) {
                    double childCost = cc.cost + (weighted ? graph.catCosts[childIndex] : 1.0);
                    frontier.add(new CatCost(cc.topLevelCat, childId, childIndex, childCost));
                }
            }
        }

        return results;
    }

    /**
     * Returns distance to specified categories for requested pages.
     * Distance is measured using shortest path in the category graph.
     *
     * @param candidateCategories   The categories to consider as candidates (e.g. those considered "top-level").
     * @param pageId                The article id we want to find.
     * @param weighted              If true, use page-rank weighted edges so paths that traverse more
     *                              general categories are penalized more highly.
     * @return                      Map with article ids as keys and distances to each category id as values.
     * @throws DaoException
     *
     */
    @Override
    public TIntDoubleMap getCategoryDistances(Set<LocalPage> candidateCategories, int pageId, boolean weighted) throws DaoException {

        Language language = candidateCategories.iterator().next().getLanguage();
        CategoryGraph graph = getGraph(language);

        Map<Integer, TIntDoubleMap> results = new HashMap<Integer, TIntDoubleMap>();

        // Indexes for goal categories
        TIntSet goalIndexes = new TIntHashSet();
        for (LocalPage p : candidateCategories) {
            int i = graph.catIdToIndex(p.getLocalId());
            if (i >= 0) goalIndexes.add(i);
        }

        // Search upwards from each page
        TIntSet visited = new TIntHashSet();

        // all we care about in CatCost for this search is the category index and cost
        PriorityQueue<CatCost> frontier = new PriorityQueue<CatCost>();
        TIntDoubleMap distances = new TIntDoubleHashMap();
        for (int catId : getCategoryIds(language, pageId)) {
            int i = graph.catIdToIndex(catId);
            if (i >= 0)  frontier.add(new CatCost(null, -1, i, graph.catCosts[i]));
        }
        while (!frontier.isEmpty() && distances.size() != candidateCategories.size()) {
            CatCost cc = frontier.poll();
            if (visited.contains(cc.catIndex)) continue;
            visited.add(cc.catIndex);
            if (goalIndexes.contains(cc.catIndex)) {
                distances.put(graph.catIndexToId(cc.catIndex), cc.cost);
            } else {

                // Ascend to unexplored parent categories.
                for (int parentIndex : graph.catParents[cc.catIndex]) {
                    if (!visited.contains(parentIndex)) {
                        double parentCost = cc.cost + (weighted ? graph.catCosts[parentIndex] : 1.0);
                        frontier.add(new CatCost(null, -1, parentIndex, parentCost));
                    }
                }
            }
        }
        return distances;
    }

    /**
     * This method should generally not be used.
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws DaoException
     */
    @Override
    public Iterable<LocalCategoryMember> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.CATEGORY_MEMBERS.LANG_ID.in(daoFilter.getLangIds()));
            }
            Cursor<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(conditions).
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<LocalCategoryMember>(result, context) {
                @Override
                public LocalCategoryMember transform(Record r) {
                    return buildLocalCategoryMember(r);
                }
            };
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException{
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.CATEGORY_MEMBERS.LANG_ID.in(daoFilter.getLangIds()));
            }
            return context.selectCount().
                    from(Tables.CATEGORY_MEMBERS).
                    where(conditions).
                    fetchOne().value1();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(Tables.CATEGORY_MEMBERS.CATEGORY_ID.eq(categoryId)).
                    and(Tables.CATEGORY_MEMBERS.LANG_ID.eq(language.getId())).
                    fetch();
            return extractIds(result, false);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Collection<Integer> getCategoryMemberIds(LocalPage localCategory) throws DaoException {
        return getCategoryMemberIds(localCategory.getLanguage(), localCategory.getLocalId());
    }

    @Override
    public Map<Integer, LocalPage> getCategoryMembers(Language language, int categoryId) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(language, categoryId);
        return localPageDao.getByIds(language, articleIds);
    }

    @Override
    public Map<Integer, LocalPage> getCategoryMembers(LocalPage localCategory) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(localCategory);
        return localPageDao.getByIds(localCategory.getLanguage(), articleIds);
    }

    @Override
    public Collection<Integer> getCategoryIds(Language language, int articleId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(Tables.CATEGORY_MEMBERS.ARTICLE_ID.eq(articleId)).
                    and(Tables.CATEGORY_MEMBERS.LANG_ID.eq(language.getId())).
                    fetch();
            return extractIds(result, true);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Collection<Integer> getCategoryIds(LocalPage localArticle) throws DaoException {
        return getCategoryIds(localArticle.getLanguage(), localArticle.getLocalId());
    }

    @Override
    public Map<Integer, LocalPage> getCategories(Language language, int articleId) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(language, articleId);
        return localPageDao.getByIds(language, categoryIds);
    }

    @Override
    public Map<Integer, LocalPage> getCategories(LocalPage localArticle) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(localArticle);
        return localPageDao.getByIds(localArticle.getLanguage(), categoryIds);
    }

    @Override
    public synchronized  CategoryGraph getGraph(Language language) throws DaoException {
        if (graphs.containsKey(language)) {
            return graphs.get(language);
        }
        String key = "cat-graph-" + language.getLangCode();
        if (cache != null) {
            CategoryGraph graph = (CategoryGraph) cache.get(key, LocalPage.class, LocalCategoryMember.class);
            if (graph != null) {
                graphs.put(language, graph);
                return graph;
            }
        }
        LocalCategoryGraphBuilder builder = new LocalCategoryGraphBuilder(localPageDao, this);
        CategoryGraph graph =  builder.build(language);
        cache.put(key, graph);
        graphs.put(language, graph);
        return graph;
    }

    private Collection<Integer> extractIds(Result<Record> result, boolean categoryIds) {
        if (result.isEmpty()) {
            return null;
        }
        Collection<Integer> pageIds = new ArrayList<Integer>();
        for(Record record : result) {
            pageIds.add(categoryIds ?
                    record.getValue(Tables.CATEGORY_MEMBERS.CATEGORY_ID) :
                    record.getValue(Tables.CATEGORY_MEMBERS.ARTICLE_ID)
            );
        }
        return pageIds;
    }

    private LocalCategoryMember buildLocalCategoryMember(Record r) {
        return new LocalCategoryMember(
                r.getValue(Tables.CATEGORY_MEMBERS.CATEGORY_ID),
                r.getValue(Tables.CATEGORY_MEMBERS.ARTICLE_ID),
                Language.getById(r.getValue(Tables.CATEGORY_MEMBERS.LANG_ID))
        );
    }

    public static class Provider extends org.wikibrain.conf.Provider<LocalCategoryMemberDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalCategoryMemberDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localCategoryMember";
        }

        @Override
        public LocalCategoryMemberDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                UniversalPageDao univDao = null;
                MetaInfoDao metaDao = getConfigurator().get(MetaInfoDao.class);
                if (metaDao.isLoaded(UniversalPage.class)) {
                    univDao = getConfigurator().get(UniversalPageDao.class);
                }
                LocalCategoryMemberSqlDao dao = new LocalCategoryMemberSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(LocalPageDao.class),
                        univDao);
                Config c = config.getConfig("topLevelCats");
                for (Map.Entry<String, ConfigValue> e : c.entrySet()) {
                    dao.addTopLevelOverride(Language.getByLangCode(e.getKey()), (String) e.getValue().unwrapped());
                }
                String cachePath = getConfig().get().getString("dao.sqlCachePath");
                File cacheDir = new File(cachePath);
                if (!cacheDir.isDirectory()) {
                    cacheDir.mkdirs();
                }
                dao.useCache(cacheDir);
                return dao;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
