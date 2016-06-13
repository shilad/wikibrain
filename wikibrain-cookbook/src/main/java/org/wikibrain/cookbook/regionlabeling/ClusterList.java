package org.wikibrain.cookbook.regionlabeling;

import java.util.*;

import gnu.trove.decorator.TIntSetDecorator;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.LocalLink.LocationType;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalCategoryMemberDao;

/**
 * Created by Anja Beth Swoap on 6/9/16.
 */
public class ClusterList {
    private ArrayList<RegionDataPoint> clusterList;
    private ArrayList<OutlinkPop> outlinks;
    private List<PageiDRank> topArticlesinCluster;
    private List<PageiDRank> top150Articles;
    private ArrayList<CategoryCount> topCats;
    private ArrayList<Integer> blacklistedIds;
    private LocalPageDao lpDao;
    private LocalLinkDao llDao;
    private LocalCategoryMemberDao lcmDao;
    private Language lang;
    private NameSpace ns;
    private static final int numArticlesToReturn = 500;
    private static final int numTopLinksConsidered = 6;


    public ClusterList(LocalPageDao l, LocalLinkDao lld, LocalCategoryMemberDao cmDao){
        clusterList = new ArrayList<RegionDataPoint>();
        outlinks = new ArrayList<OutlinkPop>();
        topArticlesinCluster = new ArrayList<PageiDRank>();
        top150Articles = new ArrayList<PageiDRank>();
        blacklistedIds = new ArrayList<Integer>();
        topCats = new ArrayList<CategoryCount>();
        lpDao = l;
        llDao = lld;
        lcmDao = cmDao;
        lang = Language.SIMPLE;
        ns = NameSpace.ARTICLE;
    }

    public ClusterList(LocalPageDao l, LocalLinkDao lld, Language lan, NameSpace n){
        clusterList = new ArrayList<RegionDataPoint>();
        outlinks = new ArrayList<OutlinkPop>();
        topArticlesinCluster = new ArrayList<PageiDRank>();
        top150Articles = new ArrayList<PageiDRank>();
        blacklistedIds = new ArrayList<Integer>();
        topCats = new ArrayList<CategoryCount>();
        lpDao = l;
        llDao = lld;
        lang = lan;
        ns = n;
    }

    public void add(RegionDataPoint p){
        clusterList.add(p);
    }

    public int size(){
        return clusterList.size();
    }

    public RegionDataPoint getPoint(int index){
        return clusterList.get(index);
    }

    //NOTE: this method written with hard-coded ids based on most-linked wikipedia articles to try to remove irrelevant results. works for english and simple english
    public void initializeBlacklist(){
        blacklistedIds.add(92288);
        blacklistedIds.add(219587);
        blacklistedIds.add(76831);
        blacklistedIds.add(4003);
        blacklistedIds.add(856);
        blacklistedIds.add(430);
        blacklistedIds.add(291);
        blacklistedIds.add(61894);
        blacklistedIds.add(2843);
        blacklistedIds.add(71721);
        blacklistedIds.add(1998);
        blacklistedIds.add(42899);
    }

    public void getTopArticles() throws Exception{
        initializeBlacklist();
        ArrayList<PageiDRank> topArticles = new ArrayList<PageiDRank>();
        for(int j = 0; j < size(); j++) {
            RegionDataPoint current = getPoint(j);
            String title = current.getArticleTitle();
            //get the corresponding page
            LocalPage p = lpDao.getByTitle(lang, ns, title);
            int l = p.getLocalId();
            LocalId lid = new LocalId(lang, l);
            //get that page's popularity
            double pop = llDao.getPageRank(lid);
            //add it to the arraylist
            PageiDRank pgidrank = new PageiDRank(lid, pop);
            topArticles.add(pgidrank);
            }

        //sort the arraylist
        Collections.sort(topArticles);

        //NOTE: this line assumes that each cluster has at least 500 entries - TODO: add edge case checking in case there aren't
        topArticlesinCluster = topArticles.subList(0, numArticlesToReturn);
    }

    public List getTopArticlesinCluster(){
        return topArticlesinCluster;
    }
    public void addToOutlinks(LocalLink l){
        //create new OutlinkPop
        OutlinkPop op = new OutlinkPop(l);
        //check if it's already in the list
        int index = outlinks.indexOf(op);
        //if yes, update value
        if(index != -1){
            //get old popularity and increment it
            int newpop = outlinks.get(index).getPopularity() + 1;
            //remove old entry and add new entry
            OutlinkPop newop = new OutlinkPop(l, newpop);
            outlinks.set(index, newop);
        }
        //if no, add new OutlinkPop
        else{
            outlinks.add(op);
        }

    }

    public void sortOutLinks(){
        Collections.sort(outlinks);
        Collections.reverse(outlinks);
    }

    public ArrayList<OutlinkPop> getOutLinks(){
        return outlinks;
    }

    public void calculateOutlinks() throws Exception{
        for(int i = 0; i < topArticlesinCluster.size(); i++){
            PageiDRank current = topArticlesinCluster.get(i);
            int id = current.getId().toInt();
            //get that page's outlinks
            Iterable<LocalLink> pageOutlinks = llDao.getLinks(lang, id, true);
            //add each outlink to this cluster's outlinks arraylist, incrementing its count
            Iterator<LocalLink> iter = pageOutlinks.iterator();
            while(iter.hasNext()){
                LocalLink curr = iter.next();
                //amke sure it's not blacklisted and add it to outlinks
                if(!blacklistedIds.contains(curr.getDestId())){
                    addToOutlinks(curr);
                }
            }
        }
    }

    public void printTopOutlinks() throws Exception{
        for(int i = 0; i < numTopLinksConsidered; i++){
            OutlinkPop o = outlinks.get(i);
            LocalLink l = o.getLink();
            int id = l.getDestId();
            LocalPage p = lpDao.getById(lang, id);
            System.out.println(p.getTitle().toString() + " count: " + o.getPopularity());
        }
    }

    //NOTE: this must be called AFTER sortOutLinks. numTopLinksConsidered is the number of links whose categories will be found
    public void calculateTopCats() throws Exception{
        for(int i = 0; i < numTopLinksConsidered; i++){
            //get current outlink destination id and convert it into page form
            int pageId= outlinks.get(i).getLink().getDestId();
            LocalId convertedId = new LocalId(lang, pageId);
            LocalPage page = lpDao.getById(convertedId);

            //grab category map for that page and convert it into set form
            Map<Integer, LocalPage> categoryMap = lcmDao.getCategories(page);
            if(categoryMap == null){
                break;
            }
            Collection<LocalPage> categories = categoryMap.values();
            HashSet categorySet = new HashSet(categories);
            //find closest set based on categorySet
            LocalPage catPage = lcmDao.getClosestCategory(page, categorySet, true);

            //if it's already in the list, increment count. otherwise, create new CategoryCount and add it
            CategoryCount catcount = new CategoryCount(catPage);
            int index = topCats.indexOf(catcount);
            //if yes, update value
            if(index != -1){
                //get old popularity and increment it
                int newcount = topCats.get(index).getCount() + 1;
                //remove old entry and add new entry
                CategoryCount newcc = new CategoryCount(catPage, newcount);
                topCats.set(index, newcc);
            }
            //if no, add new OutlinkPop
            else{
                topCats.add(catcount);
            }
        }

        Collections.sort(topCats);

    }

//NOTE: this method must be run AFTER calculateTopCats and will overwrite its curretn contents to be one category up the hierarchy
    public void goUpOneCatLevel() throws Exception{
        ArrayList<CategoryCount> newTopCats = new ArrayList<CategoryCount>();
        for(int i = 0; i < topCats.size() ; i++){
            //get current topCat page
            LocalPage page = topCats.get(i).getPage();
            //grab category map for that page and convert it into set form
            Map<Integer, LocalPage> categoryMap = lcmDao.getCategories(page);
            Collection<LocalPage> categories = categoryMap.values();
            HashSet categorySet = new HashSet(categories);
            //find closest set based on categorySet
            LocalPage catPage = lcmDao.getClosestCategory(page, categorySet, true);

            //if it's already in the list, increment count. otherwise, create new CategoryCount and add it
            CategoryCount catcount = new CategoryCount(catPage);
            int index = newTopCats.indexOf(catcount);
            //if yes, update value
            if(index != -1){
                //get old popularity and increment
                int newCount = newTopCats.get(index).getCount() + 1;
                //remove old entry and add new entry
                CategoryCount newCatCount = new CategoryCount(catPage, newCount);
                newTopCats.set(index, newCatCount);
            }
            //if no, add new OutlinkPop
            else{
                newTopCats.add(catcount);
            }
        }

        Collections.sort(newTopCats);
        topCats = newTopCats;

    }

    //must be called after topCats has been calculated and has gone up as many levels as desired - this MODIFIES topCats to contain page-evaluated categories with new counts
    public void evaluateTopCats() throws Exception {
        /* OLD METHOD CODE WITH NULL ISSUE - SAVED IN CASE NEW METHOD DOESN'T WORK
        //create set view of top categories
        HashSet topCategories = new HashSet();
        for(int i = 0; i < topCats.size(); i++){
            topCategories.add(topCats.get(i).getPage());
        }

        top150Articles = topArticlesinCluster.subList(0, 150);
        ArrayList<CategoryCount> evaluatedTopCats = new ArrayList<>();

        for(int i = 0; i < top150Articles.size(); i++){
            PageiDRank prank = top150Articles.get(i);
            LocalPage equivPage = lpDao.getById(prank.getId());
            LocalPage topCatPage = lcmDao.getClosestCategory(equivPage, topCategories, false);
            //hacky way to get around the fact that it's null and just see if it works
            if(topCatPage != null){

                CategoryCount catcount = new CategoryCount(topCatPage);

                int index = evaluatedTopCats.indexOf(catcount);

                //if yes, update value
                if(index != -1){
                    //get old popularity and increment
                    int newCount = evaluatedTopCats.get(index).getCount() + 1;
                    //remove old entry and add new entry
                    CategoryCount newCatCount = new CategoryCount(topCatPage, newCount);
                    evaluatedTopCats.set(index, newCatCount);
                }
                //if no, add new OutlinkPop
                else{
                    evaluatedTopCats.add(catcount);
                }
            }

        }

        Collections.sort(evaluatedTopCats);
        Collections.reverse(evaluatedTopCats);
        topCats = evaluatedTopCats;

        */

        //create set of candidate categories from top categories generated from calculateTopCats and (optionally) goUpOneLevel
        HashSet topCategories = new HashSet();
        for(int i = 0; i < topCats.size(); i++){
            topCategories.add(topCats.get(i).getPage());
        }

        //create arraylist of top 150 pages (by pageRank)
        top150Articles = topArticlesinCluster.subList(0, 125);
        //extract page ids, convert to int, and create TIntSet
        TIntHashSet pageIds = new TIntHashSet(150);

        for(int i = 0; i < top150Articles.size(); i++){
            PageiDRank p = top150Articles.get(i);
            LocalId id = p.getId();
            LocalPage pg = lpDao.getById(id);
            int pgid = pg.getLocalId();
            pageIds.add(pgid);
        }

        //call getClosestCategories - "true" boolean sets edge weighting to true. Returns Map<LocalPage, TIntDoubleMap>
        Map<LocalPage, TIntDoubleMap> categoryMap = lcmDao.getClosestCategories(topCategories, pageIds, false);

        //get the map's entry set
        Set<Map.Entry<LocalPage, TIntDoubleMap>> categoryMapSet = categoryMap.entrySet();

        //create temporary list to hold new top categories
        ArrayList<CategoryCount> newTopCategories = new ArrayList<>();

        //iterate through entry set, create CategoryCount objects, and add them to ArrayList
        Iterator<Map.Entry<LocalPage, TIntDoubleMap>> iter = categoryMapSet.iterator();
        while(iter.hasNext()){
            Map.Entry<LocalPage, TIntDoubleMap> currentCat = iter.next();

            //use getValue().size() to determine # of articles that have this as their top category
            CategoryCount catcount = new CategoryCount(currentCat.getKey(), currentCat.getValue().size());

            newTopCategories.add(catcount);
        }

        //sort by count and reassign topCats to newly-evaluated categories
        Collections.sort(newTopCategories);
        Collections.reverse(newTopCategories);
        topCats = newTopCategories;


    }

    public String getTopCat(){
        return topCats.get(0).getPage().getTitle().getCanonicalTitle().substring(9);
    }

    public void printTopCats(){
        for(int i = 0; i < topCats.size(); i++){
            System.out.println(topCats.get(i));
        }
    }

}
