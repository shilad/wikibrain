package org.wikapidia.sr.category;

import com.typesafe.config.Config;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.dao.sql.SqlCache;
import org.wikapidia.core.lang.Language;
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
public class MonolingualCategoryGraphHelper {

    private static final Logger LOG = Logger.getLogger(MonolingualCategoryGraphHelper.class.getName());

    private Language language;
    private LocalPageDao pageHelper;
    private LocalCategoryMemberDao catHelper;
    private CategoryGraph graph;
    private SqlCache sqlCache;

    /**
     */
    public MonolingualCategoryGraphHelper(Language language, LocalPageDao pageHelper, LocalCategoryMemberDao catHelper, SqlCache sqlCache){
        this.language=language;
        this.pageHelper=pageHelper;
        this.catHelper=catHelper;
        this.sqlCache=sqlCache;
        this.graph = null;
    }

    public CategoryGraph graph(){
        return graph;
    }

    public void init() throws DaoException {
        try {
            if (sqlCache!=null){
                CategoryGraph graph = (CategoryGraph)sqlCache.get(language.getLangCode()+"-CategoryGraph",LocalCategoryMember.class);
                if (graph==null){
                    throw new DaoException();
                }
                this.graph=graph;
            } else {
                manualInit();
            }
        } catch (DaoException e){
            manualInit();
            sqlCache.put(language.getLangCode()+"-CategoryGraph",graph);
        }
    }

    private void manualInit() throws DaoException{
        loadCategories();
        buildGraph();
        computePageRanks();
    }

    private void loadCategories() throws DaoException {
        LOG.info("loading categories...");
        CategoryGraph graph = new CategoryGraph();
        graph.catIndexes = new TIntIntHashMap();
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
        this.graph=graph;
        LOG.info("finished loading " + graph.cats.length + " categories");
    }

    private void buildGraph() throws DaoException {
        LOG.info("building category graph");
        CategoryGraph graph = this.graph;
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

        DaoFilter filter = new DaoFilter().setLanguages(language);
        for (LocalCategoryMember lcm : catHelper.get(filter)) {
            int catIndex1 = graph.getCategoryIndex(lcm.getArticleId());     // cat index for page (probably -1)
            int catIndex2 = graph.getCategoryIndex(lcm.getCategoryId());    // cat index for cat
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
        for (LocalCategoryMember lcm : catHelper.get(filter)) {
            int catIndex1 = graph.getCategoryIndex(lcm.getArticleId());     // cat index for page (probably -1)
            int catIndex2 = graph.getCategoryIndex(lcm.getCategoryId());    // cat index for cat
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
        this.graph=graph;
        LOG.info("loaded " + totalEdges + " edges in category graph");
    }

    public void computePageRanks() {
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

        LOG.info("Min cat cost: " + graph.minCost);
        LOG.info("Top cat costs: " + b.toString());
    }


    public static class Provider extends org.wikapidia.conf.Provider<MonolingualCategoryGraphHelper> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return MonolingualCategoryGraphHelper.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public MonolingualCategoryGraphHelper get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (runtimeParams==null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("MonolingualCategoryGraphHelper requires 'language' runtime parameter.");
            }

            Language language = Language.getByLangCode(runtimeParams.get("language"));

            LocalPageDao pageDao = getConfigurator().get(LocalPageDao.class);
            LocalCategoryMemberDao memberDao = getConfigurator().get(LocalCategoryMemberDao.class);
            SqlCache sqlCache = null;
            try {
                MetaInfoDao metaInfoDao = getConfigurator().get(MetaInfoDao.class);
                String cachePath = getConfig().get().getString("dao.sqlCachePath");
                File cacheDir = new File(cachePath);
                sqlCache = new SqlCache(metaInfoDao,cacheDir);
            } catch (DaoException e){}
            MonolingualCategoryGraphHelper graphHelper = new MonolingualCategoryGraphHelper(language,pageDao,memberDao,sqlCache);



            return graphHelper;
        }
    }
}
