package org.wikibrain.cookbook.regionlabeling;

import org.wikibrain.core.model.LocalLink;
/**
 * Created by Anja Beth Swoap on 6/9/16.
 */
public class OutlinkPop implements Comparable<OutlinkPop>{
    public LocalLink link;
    public int popularity;

    public OutlinkPop(LocalLink l){
        link = l;
        popularity = 1;
    }

    public OutlinkPop(LocalLink l, int p){
        link = l;
        popularity = p;
    }

    public int getPopularity(){
        return popularity;
    }

    public LocalLink getLink(){
        return link;
    }

    public void incrementPopularity(){
        popularity += 1;
    }

    public int compareTo(OutlinkPop o){
       return Integer.compare(popularity, o.getPopularity());
    }

    public String toString(){
        int x = link.getDestId();

        return "id: " + x;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OutlinkPop)) {
            return false;
        }
        OutlinkPop op = (OutlinkPop) obj;

        return (link.getDestId() == op.getLink().getDestId());
    }

}
