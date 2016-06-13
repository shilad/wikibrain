package org.wikibrain.cookbook.regionlabeling;

import org.wikibrain.core.lang.LocalId;

/**
 * Created by Anja Beth Swoap on 6/10/16.
 */
public class PageiDRank implements Comparable<PageiDRank> {
    public LocalId id;
    public double rank;

    public PageiDRank(LocalId i, double r){
        id = i;
        rank = r;
    }

    public LocalId getId(){
        return id;
    }

    public double getRank(){
        return rank;
    }

    public int compareTo(PageiDRank p){
        return Double.compare(rank, p.getRank());
    }
}
