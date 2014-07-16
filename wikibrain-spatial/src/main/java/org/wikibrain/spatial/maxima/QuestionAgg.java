package org.wikibrain.spatial.maxima;

/**
 * Created by harpa003 on 7/15/14.
 */
public class QuestionAgg {
    String name1;
    String name2;
    int id1;
    int id2;
    double km;
    int graphDist;
    double compSR;
    int expectedFF=0;
    int expectedFU=0;
    int expectedUU=0;
    int realFF=0;
    int realFU=0;
    int realUU=0;
    double avgFFsr;
    double avgFUsr;
    double avgUUsr;
    double overallAvg;
    double diffFFUU;
    double diffCompHumFF;
    double diffCompHumFU;
    double diffCompHumUU;
    double diffCompHum;
    final int FF=0,FU=1,UU=2;



    public QuestionAgg(){
    }

    public QuestionAgg(Question q){
        this.name1=q.name1;
        this.name2=q.name2;
        this.id1=q.id1;
        this.id2=q.id2;
        this.km=q.km;
        this.graphDist=q.graphDist;
        this.compSR=q.compSR;
        increaseExpected(q);
        increaseReal(q);
    }

    public void increaseReal(Question q) {
        if(q.getType()==FF){
            realFF++;
        } else if(q.getType()==UU){
            realUU++;
        } else if(q.getType()==FU){
            realFU++;
        }
    }

    public void increaseExpected(Question q){
        if(q.known1 && q.known2){
            expectedFF++;
        } else if(!q.known1 && !q.known2){
            expectedUU++;
        } else if((q.known1 && !q.known2) || (!q.known1 && q.known2)){
            expectedFU++;
        }
    }

    @Override
    public String toString(){
        return name1+"\t"+name2+"\t"+expectedFF+"\t"+expectedFU+"\t"+expectedUU+"\t"+realFF+"\t"+realFU+"\t"+realUU+"\t"+avgFFsr+"\t"+avgFUsr+"\t"+avgUUsr+"\t"+diffFFUU+"\t"+diffCompHumFF+"\t"+diffCompHumFU+"\t"+diffCompHumUU+"\t"+diffCompHum+"\t"+compSR+"\t"+overallAvg;
    }

    public void convertAvg() {
        avgFFsr=(avgFFsr-1.0)/4.0;
        avgFUsr=(avgFUsr-1.0)/4.0;
        avgUUsr=(avgUUsr-1.0)/4.0;
        overallAvg=(overallAvg-1.0)/4.0;
    }
}
