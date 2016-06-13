package org.wikibrain.cookbook.kmeans;

/**
 * Created by research on 6/6/16.
 */
public class titleAndPop implements Comparable<titleAndPop>{
    private String title;
    private double pageRank;

    public titleAndPop(String s, double p){
        title = s;
        pageRank = p;

    }

    public double getPageRank(){
        return pageRank;
    }

    public String getTitle(){
        return title;
    }

    public int compareTo(titleAndPop t){



        if(pageRank > t.getPageRank()){
            return 1;
        }
        else if(pageRank < t.getPageRank()){
            return -1;
        }
        else{
            return 0;
        }
    }
}
