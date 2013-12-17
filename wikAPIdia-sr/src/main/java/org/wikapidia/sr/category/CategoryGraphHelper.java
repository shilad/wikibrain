package org.wikapidia.sr.category;

import com.typesafe.config.Config;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.dao.sql.SqlCache;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds and stores a directed graph among categories and pages.
 */
public class CategoryGraphHelper {

    private static final Logger LOG = Logger.getLogger(CategoryGraphHelper.class.getName());

    protected LocalPageDao pageHelper;
    protected LocalCategoryMemberDao catHelper;
    protected LanguageSet languages;
    protected Map<Language,CategoryGraph> graphs;
    protected SqlCache sqlCache;

    /**
     * @throws java.io.IOException
     */
    public CategoryGraphHelper(LocalPageDao pageHelper, LocalCategoryMemberDao catHelper, SqlCache sqlCache, LanguageSet languages){
        this.pageHelper=pageHelper;
        this.catHelper=catHelper;
        this.languages=languages;
        this.sqlCache=sqlCache;
        this.graphs = new HashMap<Language,CategoryGraph>();
    }

    public CategoryGraph graph(Language language){
        return graphs.get(language);
    }

    public void init() throws DaoException {
        for (Language language: languages){
            try {
                if (sqlCache!=null){
                    CategoryGraph graph = (CategoryGraph)sqlCache.get(language.getLangCode()+"-CategoryGraph",LocalCategoryMember.class);
                    if (graph==null){
                        throw new DaoException();
                    }
                    graphs.put(language,graph);
                } else {
                    manualInit(language);
                }
            } catch (DaoException e){
                manualInit(language);
                sqlCache.put(language.getLangCode()+"-CategoryGraph",graphs.get(language));
            }
        }
    }

    private void manualInit(Language language) throws DaoException{
        loadCategories(language);
        buildGraph(language);
        calculateTopLevelCategories(language);
        computePageRanks(language);
    }

    private void loadCategories(Language language) throws DaoException {
        LOG.info("loading categories...");
        CategoryGraph graph = new CategoryGraph(language);
        graph.catIndexes = new HashMap<Integer, Integer>();
        List<String> catList = new ArrayList<String>();
        Iterable<LocalPage> catIter = pageHelper.get(new DaoFilter()
                .setNameSpaces(NameSpace.CATEGORY)
                .setLanguages(language)
        );
        for (LocalPage cat : catIter){
            catList.add(cat.getTitle().getCanonicalTitle());
            graph.catIndexes.put (cat.getLocalId(),graph.catIndexes.size());
        }
        graph.cats = catList.toArray(new String[0]);
        graphs.put(language,graph);
        LOG.info("finished loading " + graph.cats.length + " categories");
    }

    private void buildGraph(Language language) throws DaoException {
        LOG.info("building category graph");
        CategoryGraph graph = graphs.get(language);
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
        int numCatPages[] = new int[graph.catIndexes.size()];
        DaoFilter filter = new DaoFilter().setLanguages(language);
        for (LocalPage page : (Iterable<LocalPage>) pageHelper.get(filter)) {
            int catIndex = graph.getCategoryIndex(page.getLocalId());
            Map<Integer, LocalCategory> cats = catHelper.getCategories(page);
            if (cats!=null){
                for (Integer cat: cats.keySet()) {
                    int catIndex2 = graph.getCategoryIndex(cat);
                    if (catIndex >= 0 && catIndex2 >= 0) {
                        numCatChildren[catIndex2]++;
                    } else if (catIndex2>=0) {
                        numCatPages[catIndex2]++;
                    }
                    totalEdges++;
                }
            }
        }

        // allocate space
        for (int i = 0; i < graph.catIndexes.size(); i++) {
            graph.catPages[i] = new int[numCatPages[i]];
            graph.catChildren[i] = new int[numCatChildren[i]];
        }

        // fill it
        for (LocalPage page : (Iterable<LocalPage>) pageHelper.get(filter)) {
            int catId1 = -1;
            Map<Integer, LocalCategory> cats = catHelper.getCategories(page);
            if (cats!=null){
                if (page.getNameSpace()==NameSpace.CATEGORY) {
                    catId1 = graph.getCategoryIndex(page.getLocalId());
                    graph.catParents[catId1] = new int[cats.size()];
                }
                int i=0;
                for (Integer cat : cats.keySet()) {
                    int catId2 = graph.getCategoryIndex(cat);
                    if (catId1 >= 0 && catId2>=0) {
                        graph.catChildren[catId2][--numCatChildren[catId2]] = catId1;
                        graph.catParents[catId1][i++] = catId2;
                    } else if (catId2>=0){
                        graph.catPages[catId2][--numCatPages[catId2]] = page.getLocalId();
                    }
                }
            }
        }
        for (int n : numCatChildren) { assert(n == 0); }
        for (int n : numCatPages) { assert(n == 0); }
        graphs.put(language,graph);
        LOG.info("loaded " + totalEdges + " edges in category graph");
//        for (int i = 0; i < catChildren.length; i+= 10000) {
//            System.err.println("info for cat " + i + " " + cats[i]);
//            System.err.println("\tparents:" + Arrays.toString(catParents[i]));
//            System.err.println("\tchildren:" + Arrays.toString(catChildren[i]));
//            System.err.println("\tpages:" + Arrays.toString(catPages[i]));
//        }
    }

    public void computePageRanks(Language language) {
        LOG.info("computing category page ranks...");
        CategoryGraph graph = graphs.get(language);

        // initialize page rank
        long sumCredits = graph.catPages.length;    // each category gets 1 credit to start
        for (int i = 0; i < graph.catPages.length; i++) {
            sumCredits += graph.catPages[i].length; // one more credit per page that references it.
        }
        for (int i = 0; i < graph.catPages.length; i++) {
            graph.catCosts[i] = (1.0 + graph.catPages[i].length) / sumCredits;
        }

        for (int i = 0; i < 20; i++) {
            LOG.log(Level.INFO, "performing page ranks iteration {0}.", i);
            double error = graph.onePageRankIteration();
            LOG.log(Level.INFO, "Error for iteration is {0}.", error);
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

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < 20; i++) {
            int j = sortedIndexes[i];
            b.append("" + i + ". " + graph.cats[j] + "=" + graph.catCosts[j]);
            b.append(", ");
        }
        graph.minCost = graph.catCosts[sortedIndexes[sortedIndexes.length - 1]];
        graphs.put(language,graph);

        LOG.info("Min cat cost: " + graph.minCost);
        LOG.info("Top cat costs: " + b.toString());
    }

    public void dump(BufferedWriter writer, Language language) throws IOException {
        CategoryGraph graph = graphs.get(language);

        writer.write("\n\nNon-orphaned category hierarchy:\n");
        for (int i = 0; i < graph.cats.length; i++) {
            if (graph.isUsefulCat(i)) {
                writer.write(
                        "id=" + i + ", " + graph.cats[i] +
                        ", parents=" + graph.catIndexesToString(graph.catParents[i]) +
                        ", children=" + graph.catIndexesToString(graph.catChildren[i]) + "\n");
            }
        }

        Integer sortedIndexes[] = new Integer[graph.catCosts.length];
        double nonUsefulPageRank = Double.MAX_VALUE;
        for (int i = 0; i < graph.catParents.length; i++) {
            sortedIndexes[i] = i;
            if (!graph.isUsefulCat(i)) {
                if (nonUsefulPageRank == Double.MAX_VALUE) {
                    nonUsefulPageRank = graph.catCosts[i];
                } else {
                    assert(nonUsefulPageRank == graph.catCosts[i]);
                }
            }
        }
        writer.write("\n\nPage ranks of non-useful cats: " + nonUsefulPageRank + "\n");
        final double[] costs = graph.catCosts;
        Arrays.sort(sortedIndexes, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                Double pr1 = costs[i1];
                Double pr2 = costs[i2];
                return -1 * pr1.compareTo(pr2);
            }
        });
        writer.write("\n\nPage ranks of useful cats:\n");
        for (int i = 0; i < sortedIndexes.length; i++) {
            int j = sortedIndexes[i];
            if (graph.isUsefulCat(j)) {
                writer.write("" + i + ". " + " i=" + j + ", " +
                        graph.cats[j] + "=" + graph.catCosts[j] + "\n");
            }
        }

        writer.write("\n\nPages to non-orphaned categories\n");
        TIntObjectHashMap<TIntArrayList> pagesToCats = new TIntObjectHashMap<TIntArrayList>();
        for (int i = 0; i < graph.catPages.length; i++) {
            if (graph.isUsefulCat(i)) {
                for (int j : graph.catPages[i]) {
                    if (!pagesToCats.containsKey(j)) {
                        pagesToCats.put(j, new TIntArrayList());
                    }
                    pagesToCats.get(j).add(i);
                }
            }
        }

        int pageIds[] = pagesToCats.keys();
        Arrays.sort(pageIds);
/*        for (int wpId : pageIds) {
            writer.write(helper.wpIdToTitle(wpId) +
                    " (id=" + wpId + ") " +
                    ": " + catIndexesToString(pagesToCats.get(wpId).toArray())
                    + "\n");
        }*/
    }

    private void calculateTopLevelCategories(Language language) throws DaoException {
        LOG.info("marking top level categories off-limits.");
        CategoryGraph graph = graphs.get(language);
        int numSecondLevel = 0;
        graph.topLevelCategories = new HashSet<Integer>();
        for (String name : TOP_LEVEL_CATS) {
            int catId = pageHelper.getIdByTitle(new Title("Category:"+name,language));
            int catIndex = graph.getCategoryIndex(catId);
            if (catIndex >= 0) {
                graph.topLevelCategories.add(catIndex);
                for (int ci : graph.catChildren[catIndex]) {
//                    topLevelCategories.add(ci);
                    numSecondLevel++;
                }
            }
        }
        graphs.put(language,graph);
        LOG.log(Level.INFO, "marked {0} top-level and {1} second-level categories.",
                new Object[] {TOP_LEVEL_CATS.length, numSecondLevel} );
    }

    public static String [] TOP_LEVEL_CATS = {
            "Agriculture", "Applied Sciences", "Arts", "Belief", "Business", "Chronology", "Computers",
            "Culture", "Education", "Environment", "Geography", "Health", "History", "Humanities",
            "Language", "Law", "Life", "Mathematics", "Nature", "People", "Politics", "Science", "Society",

            "Concepts", "Life", "Matter", "Society",

    };



    public static class Provider extends org.wikapidia.conf.Provider<CategoryGraphHelper> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return CategoryGraphHelper.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public CategoryGraphHelper get(String name, Config config) throws ConfigurationException {
            LanguageSet languages = getConfigurator().get(LanguageSet.class);
            LocalPageDao pageDao = getConfigurator().get(LocalPageDao.class);
            LocalCategoryMemberDao memberDao = getConfigurator().get(LocalCategoryMemberDao.class);
            SqlCache sqlCache = null;
            try {
                MetaInfoDao metaInfoDao = getConfigurator().get(MetaInfoDao.class);
                String cachePath = getConfig().get().getString("dao.sqlCachePath");
                File cacheDir = new File(cachePath);
                sqlCache = new SqlCache(metaInfoDao,cacheDir);
            } catch (DaoException e){}
            CategoryGraphHelper graphHelper = new CategoryGraphHelper(pageDao,memberDao,sqlCache,languages);



            return graphHelper;
        }
    }
}
