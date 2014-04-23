package org.wikibrain.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that maintains counts when adding things to HashMaps. That is, when an object is added n times
 * to a HashMap, it will maintain a count for that object equal to n. Somewhat analogous to MultiHashmap,
 * but likely has a lower memory footprint. It's also less overkill for the simple task of maintaining counts.
 */
public class SummingHashMap<T1> extends HashMap<T1,Double> {

    @Override
    public Double put(T1 key, Double value){

        Double newValue;
        Double oldValue = null;
        if (this.containsKey(key)){
            oldValue = this.get(key);
            newValue = value + oldValue;
        }else{
            newValue = value.doubleValue();
        }

        super.put(key, newValue);
        return oldValue;


    }

    public void addMap(Map<T1, Double> map){

        for (T1 key : map.keySet()){
            this.put(key, map.get(key));
        }

    }

    /**
     * Increments the count for @code key by 1.
     * @param key The key for which the count will be incremented by one.
     */
    public void incrementValue(T1 key){
        addValue(key,1.0);
    }

    public void addValue(T1 key, Integer value){
        Double temp = new Double(value.doubleValue());
        this.put(key, temp);
    }

    public void addValue(T1 key, Double value){
        this.put(key, value);
    }


    public void divideBy(double denominator){


        for (T1 key : this.keySet()){
            double newVal = this.get(key)/denominator;
            super.put(key, newVal);
        }
    }

    public Double getSum(){

        Double curSum = new Double(0.0);
        for (T1 curKey : this.keySet()){
            curSum = curSum + (Double)this.get(curKey);
        }


        return curSum;

    }

    public void normalize(){

        Double sum = this.getSum();
        Double newValue;
        for (T1 curKey: this.keySet()){
            newValue = (Double)this.get(curKey);
            newValue = newValue/sum;
            super.put(curKey, newValue);
        }

    }

    public T1 getMaximum(){

        Double curMaxVal = Double.MIN_VALUE;
        T1 curMaxKey = null;

        for (T1 curKey : this.keySet()){

            if ((Double)this.get(curKey) > curMaxVal){
                curMaxVal = (Double)this.get(curKey);
                curMaxKey = curKey;
            }
        }

        return curMaxKey;

    }

    public SummingHashMap<T1> getCopy(){

        SummingHashMap<T1> rVal = new SummingHashMap<T1>();
        for (T1 key : this.keySet()){
            rVal.put(key, this.get(key));
        }

        return rVal;

    }

}
