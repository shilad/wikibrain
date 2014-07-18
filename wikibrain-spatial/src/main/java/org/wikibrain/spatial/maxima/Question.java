package org.wikibrain.spatial.maxima;

import org.wikibrain.utils.ObjectDb;

/**
 * Created by harpa003 on 7/14/14.
 */
public class Question {
    int userid;
    int sr;
    int fam1;
    int fam2;
    int questionNumb;
    int id1;
    int id2;
    int scale1;
    int scale2;
    int pop1;
    int pop2;
    boolean known1;
    boolean known2;
    String name1;
    String name2;
    double km;
    int graphDist;
    Double compSR;
    int numbOfKKTimesAsked=0;
    int numbOfUUTimesAsked=0;
    int numbOfKUTimesAsked=0;
    final int FF=0,FU=1,UU=2;


    public Question(){
    }

    public int getType(){
        if(fam1>=4 && fam2>=4){ //4,5
            return FF;
        }
        if(fam1<=3 && fam2<=3 && fam1>1 && fam2>1){ //2,3
            return UU;
        }
        if((fam1>3 && fam2<=3 && fam2>1) || (fam1<=3 && fam1>1  && fam2>3)){
            return FU;
        }
        return -100;
    }

    public void setKnown1(String known1) {
        if(known1.equalsIgnoreCase("true")){
            this.known1=true;
        } else if(known1.equalsIgnoreCase("false")){
            this.known1=false;
        }else System.out.println("uh oh "+ known1);
    }


    public void setKnown2(String known2) {
        if(known2.equalsIgnoreCase("true")){
            this.known2=true;
        } else if(known2.equalsIgnoreCase("false")){
            this.known2=false;
        }else System.out.println("uh oh "+ known2);
    }

//    @Override
//    public boolean equals(Object o){
//        Question two= (Question) o;
//        if((this.id1==two.id1 && this.id2==two.id2) || (this.id2==two.id1 && this.id1== two.id2)){
//            return true;
//        }
//        return false;
//    }
//
//    private String getCombinedTitle() {
//        String firstStr = this.name1;
//        String secondStr = this.name2;
//        String combinedTitle = null;
//        if (firstStr.compareTo(secondStr) < 0) {
//            combinedTitle = firstStr + secondStr;
//        } else {
//            combinedTitle = secondStr + firstStr;
//        }
//
//        return combinedTitle;
//    }
//
//    @Override
//    public int hashCode() {
//        return getCombinedTitle().hashCode();
//    }
}
