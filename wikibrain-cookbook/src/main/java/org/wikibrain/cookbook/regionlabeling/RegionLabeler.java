package org.wikibrain.cookbook.regionlabeling;

import java.io.FileReader;
import java.io.IOException;

import java.lang.Math;

import java.util.ArrayList;
import java.util.Collections;

import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.LocalLinkDao;

import org.wikibrain.core.model.LocalPage;

import org.wikibrain.core.lang.Language;

import au.com.bytecode.opencsv.CSVReader;
/**
 * Created by Anja Beth Swoap on 6/9/16.
 */
public class RegionLabeler {

    private ClusterList cluster1;
    private ClusterList cluster2;
    private ClusterList cluster3;
    private ClusterList cluster4;
    private ClusterList cluster5;
    private ClusterList cluster6;
    private ClusterList cluster7;
    private ClusterList cluster8;
    private ClusterList cluster9;
    private ClusterList cluster10;
    private LocalPageDao lpDao;


    public RegionLabeler(LocalPageDao lp, LocalLinkDao lld, LocalCategoryMemberDao lcmd){
        cluster1 = new ClusterList(lp, lld, lcmd, 1);
        cluster2 = new ClusterList(lp, lld, lcmd, 2);
        cluster3 = new ClusterList(lp, lld, lcmd, 3);
        cluster4 = new ClusterList(lp, lld, lcmd, 4);
        cluster5 = new ClusterList(lp, lld, lcmd, 5);
        cluster6 = new ClusterList(lp, lld, lcmd, 6);
        cluster7 = new ClusterList(lp, lld, lcmd, 7);
        cluster8 = new ClusterList(lp, lld, lcmd, 8);
        cluster9 = new ClusterList(lp, lld, lcmd, 9);
        cluster10 = new ClusterList(lp, lld, lcmd, 10);
        lpDao = lp;

    }

    /**
     * Switch statement to get a particular cluster
     * @param i
     * @return
     */
    public ClusterList getCluster(int i) {
        switch (i) {
            case 1:
                return (cluster1);
            case 2:
                return (cluster2);
            case 3:
                return (cluster3);
            case 4:
                return (cluster4);
            case 5:
                return (cluster5);
            case 6:
                return (cluster6);
            case 7:
                return (cluster7);
            case 8:
                return (cluster8);
            case 9:
                return (cluster9);
            case 10:
                return (cluster10);
            default:
                return null;

        }
    }

    /**
     * Switch statement to assign points to clusters neatly
     * @param p
     */
    public  void assignCluster(RegionDataPoint p){
        switch(p.getClusterId()){
            case 1:
                cluster1.add(p);
                break;
            case 2:
                cluster2.add(p);
                break;
            case 3:
                cluster3.add(p);
                break;
            case 4:
                cluster4.add(p);
                break;
            case 5:
                cluster5.add(p);
                break;
            case 6:
                cluster6.add(p);
                break;
            case 7:
                cluster7.add(p);
                break;
            case 8:
                cluster8.add(p);
                break;
            case 9:
                cluster9.add(p);
                break;
            case 10:
                cluster10.add(p);
                break;
        }
    }

    /**
     * Switch statement to get a particular cluster to print
     * @param cluster
     */
    public  void printCluster(int cluster){
        switch(cluster){
            case 1:
                printOneCluster(cluster1);
                break;
            case 2:
                printOneCluster(cluster2);
                break;
            case 3:
                printOneCluster(cluster3);
                break;
            case 4:
                printOneCluster(cluster4);
                break;
            case 5:
                printOneCluster(cluster5);
                break;
            case 6:
                printOneCluster(cluster6);
                break;
            case 7:
                printOneCluster(cluster7);
                break;
            case 8:
                printOneCluster(cluster8);
                break;
            case 9:
                printOneCluster(cluster9);
                break;
            case 10:
                printOneCluster(cluster10);
                break;
        }
    }

    /**
     * Prints all points in a particular cluster (for debugging)
     * @param c
     */
    public void printOneCluster(ClusterList c){
        for(int i = 0; i < c.size(); i++){
            System.out.println(c.getPoint(i));
        }
    }

    /**
     * Counts # of outlinks from all articles within all clusters - used in calculating idf
     * @return
     */

    public int getTotalLinks(){
        int totalLinks = 0;
        for(int i = 0; i < 10; i++){
            ClusterList currList = getCluster(i + 1);
            totalLinks += currList.countOutlinks();
        }

        return totalLinks;
    }

    /**
     *  Find total # of outlinks to a particular page from all clusters - used in calculating idf
     * @param op
     * @return count of all times this article has been outlinked to by all clusters
     */
    public int searchAllOutlinks(OutlinkPop op){
        int total = 0;
        for(int i = 0; i < 10; i++){
            ClusterList currList = getCluster(i + 1);
            total += currList.searchOutlinks(op);
        }

        return total;
    }

    /**
     * Calculate idf for a particular OutlinkPop
     */

    public double calculateIDF(OutlinkPop op){
        double freq = searchAllOutlinks(op);
        double total = getTotalLinks();

        double idf = Math.log(total/freq);
        return idf;
    }

    /**
     * Reorder outlinks of a particular cluster by tf-idf
     */

    public void doTfIdf (int clusterNumber) throws Exception{
        ClusterList cluster = getCluster(clusterNumber);
        ArrayList<OutlinkPop> tempOutlinks = cluster.getOutlinks();
        ArrayList<OutlinkTFIDF> tfidfs = new ArrayList<OutlinkTFIDF>();
        //go through all outlinks, calculate their tf-idf, and add them (with tf-idf as popularity) to a new list of outlinkpops
        for(int i = 0; i < tempOutlinks.size(); i++){
            OutlinkPop currentOlP = tempOutlinks.get(i);
            double tf = cluster.calculateTF(currentOlP);
            double idf = calculateIDF(currentOlP);
            double tfidf = tf * idf;
            /* this was for debugging int id = currentOlP.getLink().getDestId();
            LocalPage p = lpDao.getById(Language.SIMPLE, id);
            String title = p.getTitle().getCanonicalTitle();
            System.out.println(title + "tfidf: " + tfidf);
            */
            OutlinkTFIDF oltf = new OutlinkTFIDF(currentOlP.getLink(), tfidf);
            tfidfs.add(oltf);
        }

        //sort new list
        Collections.sort(tempOutlinks);
        Collections.reverse(tempOutlinks);

        //set cluster tfidfs to created idfs list
        cluster.setTfidfs(tfidfs);

    }

    public void doAllTfIdf() throws Exception {
        for(int i = 0; i < 10; i++){
            doTfIdf(i + 1);
        }
    }
}
