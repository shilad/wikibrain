package org.wikapidia.sr.category;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds and stores a directed graph among categories and pages.
 */
public class CategoryGraph {

    private static final Logger LOG = Logger.getLogger(CategoryGraph.class.getName());

    protected LocalPageDao pageHelper;
    protected LocalCategoryMemberDao catHelper;
    protected Language language;

    protected Map<Integer,Integer> catIndexes;
    protected Set<Integer> topLevelCategories;

    protected double[] catCosts;  // the cost of travelling through each category
    protected int[][] catParents;
    protected int[][] catPages;
    protected int[][] catChildren;
    protected String[] cats;
    protected double minCost = -1;

    /**
     * @throws java.io.IOException
     */
    public CategoryGraph(LocalPageDao pageHelper, LocalCategoryMemberDao catHelper, Language language){
        this.pageHelper=pageHelper;
        this.catHelper=catHelper;
        this.language=language;
    }

    public void init() throws DaoException {
        loadCategories();
        buildGraph();
        calculateTopLevelCategories();
        computePageRanks();
    }

    private void loadCategories() throws DaoException {
        LOG.info("loading categories...");
        this.catIndexes = new HashMap<Integer, Integer>();
        List<String> catList = new ArrayList<String>();
        Iterable<LocalPage> catIter = pageHelper.get(new DaoFilter()
                .setNameSpaces(NameSpace.CATEGORY)
                .setLanguages(language)
        );
        for (LocalPage cat : catIter){
            catList.add(cat.getTitle().getCanonicalTitle());
            catIndexes.put (cat.getLocalId(),catIndexes.size());
        }
        cats = catList.toArray(new String[0]);
        LOG.info("finished loading " + cats.length + " categories");
    }

    private void buildGraph() throws DaoException {
        LOG.info("building category graph");
        this.catPages = new int[catIndexes.size()][];
        this.catParents = new int[catIndexes.size()][];
        this.catChildren = new int[catIndexes.size()][];
        this.catCosts = new double[catIndexes.size()];

        Arrays.fill(catPages, new int[0]);
        Arrays.fill(catParents, new int[0]);
        Arrays.fill(catChildren, new int[0]);

        // count reverse edges
        int totalEdges = 0;
        int numCatChildren[] = new int[catIndexes.size()];
        int numCatPages[] = new int[catIndexes.size()];
        DaoFilter filter = new DaoFilter().setLanguages(language);
        for (LocalPage page : (Iterable<LocalPage>) pageHelper.get(filter)) {
            int catIndex = getCategoryIndex(page.getLocalId());
            Map<Integer, LocalCategory> cats = catHelper.getCategories(page);
            if (cats!=null){
                for (Integer cat: cats.keySet()) {
                    int catIndex2 = getCategoryIndex(cat);
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
        for (int i = 0; i < catIndexes.size(); i++) {
            catPages[i] = new int[numCatPages[i]];
            catChildren[i] = new int[numCatChildren[i]];
        }

        // fill it
        for (LocalPage page : (Iterable<LocalPage>) pageHelper.get(filter)) {
            int catId1 = -1;
            Map<Integer, LocalCategory> cats = catHelper.getCategories(page);
            if (cats!=null){
                if (page.getNameSpace()==NameSpace.CATEGORY) {
                    catId1 = getCategoryIndex(page.getLocalId());
                    catParents[catId1] = new int[cats.size()];
                }
                int i=0;
                for (Integer cat : cats.keySet()) {
                    int catId2 = getCategoryIndex(cat);
                    if (catId1 >= 0 && catId2>=0) {
                        catChildren[catId2][--numCatChildren[catId2]] = catId1;
                        catParents[catId1][i++] = catId2;
                    } else if (catId2>=0){
                        catPages[catId2][--numCatPages[catId2]] = page.getLocalId();
                    }
                }
            }
        }
        for (int n : numCatChildren) { assert(n == 0); }
        for (int n : numCatPages) { assert(n == 0); }
        LOG.info("loaded " + totalEdges + " edges in category graph");
//        for (int i = 0; i < catChildren.length; i+= 10000) {
//            System.err.println("info for cat " + i + " " + cats[i]);
//            System.err.println("\tparents:" + Arrays.toString(catParents[i]));
//            System.err.println("\tchildren:" + Arrays.toString(catChildren[i]));
//            System.err.println("\tpages:" + Arrays.toString(catPages[i]));
//        }
    }

    public void computePageRanks() {
        LOG.info("computing category page ranks...");

        // initialize page rank
        long sumCredits = catPages.length;    // each category gets 1 credit to start
        for (int i = 0; i < catPages.length; i++) {
            sumCredits += catPages[i].length; // one more credit per page that references it.
        }
        for (int i = 0; i < catPages.length; i++) {
            catCosts[i] = (1.0 + catPages[i].length) / sumCredits;
        }

        for (int i = 0; i < 20; i++) {
            LOG.log(Level.INFO, "performing page ranks iteration {0}.", i);
            double error = onePageRankIteration();
            LOG.log(Level.INFO, "Error for iteration is {0}.", error);
            if (error == 0) {
                break;
            }
        }
        Integer sortedIndexes[] = new Integer[catCosts.length];
        for (int i = 0; i < catParents.length; i++) {
            catCosts[i] = 1.0/-Math.log(catCosts[i]);
            sortedIndexes[i] = i;
        }
        LOG.info("finished computing page ranks...");
        Arrays.sort(sortedIndexes, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                Double pr1 = catCosts[i1];
                Double pr2 = catCosts[i2];
                return -1 * pr1.compareTo(pr2);
            }
        });

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < 20; i++) {
            int j = sortedIndexes[i];
            b.append("" + i + ". " + cats[j] + "=" + catCosts[j]);
            b.append(", ");
        }
        minCost = catCosts[sortedIndexes[sortedIndexes.length - 1]];

        LOG.info("Min cat cost: " + minCost);
        LOG.info("Top cat costs: " + b.toString());
    }

    public void dump(BufferedWriter writer) throws IOException {

        writer.write("\n\nNon-orphaned category hierarchy:\n");
        for (int i = 0; i < cats.length; i++) {
            if (isUsefulCat(i)) {
                writer.write(
                        "id=" + i + ", " + cats[i] +
                        ", parents=" + catIndexesToString(catParents[i]) +
                        ", children=" + catIndexesToString(catChildren[i]) + "\n");
            }
        }

        Integer sortedIndexes[] = new Integer[catCosts.length];
        double nonUsefulPageRank = Double.MAX_VALUE;
        for (int i = 0; i < catParents.length; i++) {
            sortedIndexes[i] = i;
            if (!isUsefulCat(i)) {
                if (nonUsefulPageRank == Double.MAX_VALUE) {
                    nonUsefulPageRank = catCosts[i];
                } else {
                    assert(nonUsefulPageRank == catCosts[i]);
                }
            }
        }
        writer.write("\n\nPage ranks of non-useful cats: " + nonUsefulPageRank + "\n");
        Arrays.sort(sortedIndexes, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                Double pr1 = catCosts[i1];
                Double pr2 = catCosts[i2];
                return -1 * pr1.compareTo(pr2);
            }
        });
        writer.write("\n\nPage ranks of useful cats:\n");
        for (int i = 0; i < sortedIndexes.length; i++) {
            int j = sortedIndexes[i];
            if (isUsefulCat(j)) {
                writer.write("" + i + ". " + " i=" + j + ", " +
                        cats[j] + "=" + catCosts[j] + "\n");
            }
        }

        writer.write("\n\nPages to non-orphaned categories\n");
        TIntObjectHashMap<TIntArrayList> pagesToCats = new TIntObjectHashMap<TIntArrayList>();
        for (int i = 0; i < catPages.length; i++) {
            if (isUsefulCat(i)) {
                for (int j : catPages[i]) {
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

    private boolean isUsefulCat(int i) {
        return (catParents[i].length > 0 || catChildren[i].length > 0 || catPages[i].length > 1);
    }

    private String catIndexesToString(int indexes[]) {
        StringBuffer sb = new StringBuffer("[");
        for (int i : indexes) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(cats[i]);
            sb.append(" (id=");
            sb.append(""+i);
            sb.append(")");
        }
        return sb.append("]").toString();
    }

    private static final double DAMPING_FACTOR = 0.85;
    protected double onePageRankIteration() {
        double nextRanks [] = new double[catCosts.length];
        Arrays.fill(nextRanks, (1.0 - DAMPING_FACTOR) / catCosts.length);
        for (int i = 0; i < catParents.length; i++) {
            int d = catParents[i].length;   // degree
            double pr = catCosts[i];    // current page-rank
            for (int j : catParents[i]) {
                nextRanks[j] += DAMPING_FACTOR * pr / d;
            }
        }
        double diff = 0.0;
        for (int i = 0; i < catParents.length; i++) {
            diff += Math.abs(catCosts[i] - nextRanks[i]);
        }
        catCosts = nextRanks;
        return diff;
    }

    public int getCategoryIndex(int catId) {
        if (catIndexes.containsKey(catId)) {
            return catIndexes.get(catId);
        } else {
            return -1;
        }
    }

    private void calculateTopLevelCategories() throws DaoException {
        LOG.info("marking top level categories off-limits.");
        int numSecondLevel = 0;
        topLevelCategories = new HashSet<Integer>();
        for (String name : TOP_LEVEL_CATS) {
            int catId = pageHelper.getIdByTitle(new Title("Category:"+name,language));
            int catIndex = getCategoryIndex(catId);
            if (catIndex >= 0) {
                topLevelCategories.add(catIndex);
                for (int ci : catChildren[catIndex]) {
//                    topLevelCategories.add(ci);
                    numSecondLevel++;
                }
            }
        }
        LOG.log(Level.INFO, "marked {0} top-level and {1} second-level categories.",
                new Object[] {TOP_LEVEL_CATS.length, numSecondLevel} );
    }

    public static String [] TOP_LEVEL_CATS = {
            "Agriculture", "Applied Sciences", "Arts", "Belief", "Business", "Chronology", "Computers",
            "Culture", "Education", "Environment", "Geography", "Health", "History", "Humanities",
            "Language", "Law", "Life", "Mathematics", "Nature", "People", "Politics", "Science", "Society",

            "Concepts", "Life", "Matter", "Society",

    };
}
