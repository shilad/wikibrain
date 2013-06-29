package org.wikapidia.sr;

import java.util.ArrayList;
import java.util.List;

public class SRResult implements Comparable<SRResult>{
    protected int id;
    protected double value;
    protected List<Explanation> explanations;

    public SRResult(){
        explanations = new ArrayList<Explanation>();
    }

    public SRResult(Double value){
        this.value=value;
        explanations = new ArrayList<Explanation>();
    }

    public void addExplanation(Explanation explanation){
        explanations.add(explanation);
    }

    public List<Explanation> getExplanations(){return explanations;}

    public boolean isValid(){return !Double.isNaN(value);}

    public double getValue(){return value;}

    public int getId(){return id;}

    @Override
    public int compareTo(SRResult result){
        return ((Double)this.value).compareTo(result.getValue());
    }

    public void centerValue(double zeroValue){
        this.value = this.value - zeroValue;
        if (this.value < 0) this.value = 0.0;
    }

}
