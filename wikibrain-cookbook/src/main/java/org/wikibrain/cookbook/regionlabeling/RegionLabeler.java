package org.wikibrain.cookbook.regionlabeling;

import java.io.FileReader;
import java.io.IOException;

import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.LocalLinkDao;

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


    public RegionLabeler(LocalPageDao lp, LocalLinkDao lld, LocalCategoryMemberDao lcmd){
        cluster1 = new ClusterList(lp, lld, lcmd);
        cluster2 = new ClusterList(lp, lld, lcmd);
        cluster3 = new ClusterList(lp, lld, lcmd);
        cluster4 = new ClusterList(lp, lld, lcmd);
        cluster5 = new ClusterList(lp, lld, lcmd);
        cluster6 = new ClusterList(lp, lld, lcmd);
        cluster7 = new ClusterList(lp, lld, lcmd);
        cluster8 = new ClusterList(lp, lld, lcmd);
        cluster9 = new ClusterList(lp, lld, lcmd);
        cluster10 = new ClusterList(lp, lld, lcmd);

    }

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

    public void printOneCluster(ClusterList c){
        for(int i = 0; i < c.size(); i++){
            System.out.println(c.getPoint(i));
        }
    }


}
