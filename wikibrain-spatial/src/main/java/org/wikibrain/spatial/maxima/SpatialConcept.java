package org.wikibrain.spatial.maxima;

/**
 * Created by harpa003 on 6/27/14.
 */
public class SpatialConcept {
    public static enum Scale {
        COUNTRY, STATE, CITY, NATURAL, LANDMARK;
    };
    private final int universalID;
    private final String title;
    private Scale scale;
    private double heardOfProb; // Probability that you have heard of the concept but do not know is spatially
    private double knowProb; // Probability that you know the concept's location well
    private int pageRank;

    public SpatialConcept(int universalID, String title) {
        this.universalID = universalID;
        this.title = title;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
        if(scale==null){
            System.out.println();
        }
    }

    public void setPageRank(int rank){
        pageRank = rank;
    }

    public int getPageRank(){
        return pageRank;
    }

    public void setHeardOfProb(double heardOfProb) {
        this.heardOfProb = heardOfProb;
    }

    public void setKnowProb(double knowProb) {
        this.knowProb = knowProb;
    }

    public int getUniversalID() {
        return universalID;
    }

    public String getTitle() {
        return title;
    }

    public Scale getScale() {
        return scale;
    }

    public double getHeardOfProb() {
        return heardOfProb;
    }

    public double getKnowProb() {
        return knowProb;
    }

    @Override
    public String toString(){
        return title+"\t"+universalID;//+"\t"+scale+"\t"+pageRank;
    }
}
