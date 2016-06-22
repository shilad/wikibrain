package org.wikibrain.cookbook.regionlabeling;

import org.wikibrain.core.model.LocalLink;

/**
 * Created by research on 6/15/16.
 */
public class OutlinkTFIDF implements Comparable<org.wikibrain.cookbook.regionlabeling.OutlinkTFIDF> {

        public LocalLink link;
        public double tfidf;

        public OutlinkTFIDF(LocalLink l, double ti){
            link = l;
            tfidf = ti;
        }

        public double getTfidf(){
            return tfidf;
        }

        public LocalLink getLink(){
            return link;
        }

        public int compareTo(org.wikibrain.cookbook.regionlabeling.OutlinkTFIDF o){
            return Double.compare(tfidf, o.getTfidf());
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
            if (!(obj instanceof org.wikibrain.cookbook.regionlabeling.OutlinkPop)) {
                return false;
            }
            org.wikibrain.cookbook.regionlabeling.OutlinkPop op = (org.wikibrain.cookbook.regionlabeling.OutlinkPop) obj;

            return (link.getDestId() == op.getLink().getDestId());
        }

}


