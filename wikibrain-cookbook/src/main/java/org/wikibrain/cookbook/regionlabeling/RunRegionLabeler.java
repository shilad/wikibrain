package org.wikibrain.cookbook.regionlabeling;

import au.com.bytecode.opencsv.CSVReader;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.LocalLink;

import java.io.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by research on 6/9/16.
 */
public class RunRegionLabeler {

    public static void main(String[]args) throws Exception {

        //set language and namespace
        NameSpace n = NameSpace.ARTICLE;
        Language simple = Language.SIMPLE;

        //TODO: figure out how to automatically identify # of clusters?

        //set up environment
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalLinkDao llDao = conf.get(LocalLinkDao.class);
        LocalCategoryMemberDao lcmDao = conf.get(LocalCategoryMemberDao.class);

        RegionLabeler labeler = new RegionLabeler(lpDao, llDao, lcmDao);

        //set up CSV reader to ge data
        CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream("/Users/research/wikibrain/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/regionlabeling/data/data3.csv"), "UTF-8"), ',');
        FileWriter fw = new FileWriter("/Users/research/wikibrain/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/regionlabeling/data/top_categories.txt");
        FileWriter articlewr = new FileWriter("/Users/research/wikibrain/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/regionlabeling/data/top_100_articles.txt");


        //get datapoints and put them into arrayLists, one for each cluster
        String [] nextLine = reader.readNext();

        System.out.println("Reading data...");
        while((nextLine = reader.readNext()) != null){
            //NOTE: these are hard-coded in - check data file to make sure you're parsing from the right columns of the csv
            RegionDataPoint point = new RegionDataPoint(Integer.parseInt(nextLine[3]), nextLine[4]);
            labeler.assignCluster(point);
        }
        System.out.println("Done reading data.");
        reader.close();
        

        System.out.println("Processing clusters...");
        //get all clusters
        for(int i = 0; i < 10; i++){
            System.out.println("Processing cluster " + (i +1));
            ClusterList c = labeler.getCluster(i + 1);
            //get most popular articles from within cluster
            c.getTopArticles();
            //calculate outlinks for those top articles
            System.out.println("Getting outlinks...");
            c.calculateOutlinks();


            List<PageiDRank> top10 = c.getTopArticlesinCluster().subList(0, 10);
            for(int j = 0; j < 10; j++){
                PageiDRank p = top10.get(j);
                LocalId l = p.getId();
                LocalPage pg = lpDao.getById(l);
                String title = pg.getTitle().getCanonicalTitle();
                int index = title.indexOf("(simple)");
                String shortTitle;
                if(index != -1){
                    shortTitle = title.substring(0, index);
                }
                else{
                    shortTitle = title;
                }

                articlewr.append(shortTitle + "\t" + " cluster " + i + "\n");

            }


            //c.printTopOutlinks();
            /* Currently commented out for tf idf testing
            System.out.println("Sorting outlinks..." + "\n");
            c.sortOutLinks();
            //calculate categories from top outlinks
            c.calculateTopCats();
            //optional - go up one category level (not sure yet whether this helps or makes it worse)
            c.goUpOneCatLevel();
            //optional - evaluate top categories by taking top 150 cluster articles, finding the category out of topCats they're most related to, and counting that one
            c.evaluateTopCats();
            fw.append("" + i +"\t" + c.getTopCat() + "\n");
            System.out.println("Finished processing cluster " + (i+1) + "\n\n\n");

            */
        }

        articlewr.close();
        System.out.println("Finished writing article titles");

        labeler.doAllTfIdf();


        for(int i = 0; i < 10; i++){
            System.out.println("Processing cluster " + (i +1) + "\n");
            ClusterList c = labeler.getCluster(i + 1);
            c.convertTfidfs();
            //c.calculateTopCats();
            //c.evaluateTopCats();
            //c.printTopCats();
            c.printTopOutlinks();
            System.out.println("\n\n\n");
        }


        fw.close();
    }
}

/*
//FOR USE IF NEEDED: Code for manually grabbing ids to blacklist
//TEMPORARY: getting forbidden/most-linked-to articles to blacklist
        ArrayList<String> blacklistedTitles = new ArrayList<String>();
        blacklistedTitles.add("Geographic coordinate system");
        blacklistedTitles.add("United States");
        blacklistedTitles.add("International Standard Book Number");
        blacklistedTitles.add("Time zone");
        blacklistedTitles.add("United Kingdom");
        blacklistedTitles.add("List of sovereign states");

        ArrayList<LocalPage> blacklistedPages = new ArrayList<LocalPage>();

        for(int i = 0; i < blacklistedTitles.size(); i++){
            LocalPage blocked = lpDao.getByTitle(simple, n, blacklistedTitles.get(i));
            blacklistedPages.add(blocked);
        }

        for(int i = 0; i < blacklistedPages.size(); i++){

            System.out.println(blacklistedPages.get(i));
        }
        for(int i = 0; i < blacklistedPages.size(); i++){
            int blocked = blacklistedPages.get(i).getLocalId();
            System.out.println(blocked);
        }
 */