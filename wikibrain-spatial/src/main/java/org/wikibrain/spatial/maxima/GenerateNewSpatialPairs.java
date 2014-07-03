package org.wikibrain.spatial.maxima;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.spatial.cookbook.tflevaluate.InstanceOfExtractor;


import java.util.*;

/**
 * Created by harpa003 on 6/27/14.
 */
public class GenerateNewSpatialPairs {

    private List<Integer> knownIds;
    private ArrayList<Integer> unknownIds;
    HashMap<Integer,String> idsStringMap;
    private float[][] distanceMatrix;
    private float[][] srMatrix;
    private float[][] graphMatrix;
    Map<Integer, Integer> idToIndexForDistanceMatrix;
    Map<Integer, Integer> idToIndexForSRMatrix;
    Map<Integer, Integer> idToIndexForGraphMatrix;
    Map<Integer, Integer> idToScaleMap;


    public GenerateNewSpatialPairs(List<Integer> knownIdList, HashMap<Integer, String> idsStringMap, TIntSet knownIdSet, float[][] distanceMatrix, float[][] srMatrix, float[][] graphMatrix, Map<Integer, Integer> idToIndexForDistanceMatrix, Map<Integer, Integer> idToIndexForSRMatrix, Map<Integer, Integer> idToIndexForGraphMatrix, Map<Integer, Integer> idToScaleMap) {
        knownIds= knownIdList;
        unknownIds= new ArrayList<Integer>();
        this.idsStringMap=idsStringMap;
        buildUnknownIdList(knownIdSet,idsStringMap);
        this.distanceMatrix= distanceMatrix;
        this.srMatrix= srMatrix;
        this.graphMatrix=graphMatrix;
        this.idToIndexForDistanceMatrix=idToIndexForDistanceMatrix;
        this.idToIndexForSRMatrix=idToIndexForSRMatrix;
        this.idToIndexForGraphMatrix=idToIndexForGraphMatrix;
        this.idToScaleMap=idToScaleMap;

    }

    private void buildUnknownIdList(TIntSet knownIdSet, HashMap<Integer,String> idsStringMap) {
        for(Integer id:idsStringMap.keySet()){
            if(!knownIdSet.contains(id)){
                unknownIds.add(id);
            }
        }
    }

    public ArrayList<SpatialConceptPair> kkGenerateSpatialPairs(Set<SpatialConceptPair> allPreviousQList){
        ArrayList<SpatialConceptPair> toReturn= new ArrayList<SpatialConceptPair>();
        for (int i = 0; i < knownIds.size()-1; i++) {
            for (int j = i+1; j < knownIds.size() ; j++) {
                int idOne = knownIds.get(i);
                SpatialConcept one = new SpatialConcept(idOne, idsStringMap.get(idOne));
                int idTwo = knownIds.get(j);
                SpatialConcept two = new SpatialConcept(idTwo, idsStringMap.get(idTwo));
                SpatialConceptPair pair = new SpatialConceptPair(one, two);
                if ((!allPreviousQList.contains(pair)) && !toReturn.contains(pair) && checkScale(one) && checkScale(two)) {
                     setProperties(pair);
                     toReturn.add(pair);
                }
            }

        }
        return toReturn;
    }

    private boolean checkScale(SpatialConcept concept) {
        Integer scale= idToScaleMap.get(concept.getUniversalID());
        if(scale==null){
            return false;
        }
        if(scale==0){
            return false;
        } else if(scale==1){
            concept.setScale(SpatialConcept.Scale.LANDMARK);
        } else if(scale==2){
            return false;
        } else if(scale==3){
            concept.setScale(SpatialConcept.Scale.COUNTRY);
        } else if(scale==4){
            concept.setScale(SpatialConcept.Scale.STATE);
        } else if(scale==5){
            concept.setScale(SpatialConcept.Scale.CITY);
        }else if(scale==6){

            concept.setScale(SpatialConcept.Scale.NATURAL);
        }
        return true;
    }

    public ArrayList<SpatialConceptPair> kuGenerateSpatialPairs(int numbOfPairs, Set<SpatialConceptPair> allPreviousQList){
        ArrayList<SpatialConceptPair> toReturn= new ArrayList<SpatialConceptPair>();
        for (int i = 0; i < numbOfPairs ; i++) {
            int rand1= (int) (Math.random()*knownIds.size());
            int rand2= (int) (Math.random()*unknownIds.size());
            if(rand1 != rand2) {
                int idOne = knownIds.get(rand1);
                SpatialConcept one = new SpatialConcept(idOne, idsStringMap.get(idOne));
                int idTwo = unknownIds.get(rand2);
                SpatialConcept two = new SpatialConcept(idTwo, idsStringMap.get(idTwo));
                SpatialConceptPair pair = new SpatialConceptPair(one, two);
                if (!allPreviousQList.contains(pair) && !toReturn.contains(pair) && checkScale(one) && checkScale(two)) {
                    setProperties(pair);
                    toReturn.add(pair);
                } else{
                    numbOfPairs++;
                }
            }
        }
        return toReturn;
    }

    public ArrayList<SpatialConceptPair> uuGenerateSpatialPairs(int numbOfPairs, Set<SpatialConceptPair> allPreviousQList){
        ArrayList<SpatialConceptPair> toReturn= new ArrayList<SpatialConceptPair>();
        for (int i = 0; i < numbOfPairs ; i++) {
            int rand1= (int) (Math.random()*unknownIds.size());
            int rand2= (int) (Math.random()*unknownIds.size());
            if(rand1 != rand2) {
                int idOne = unknownIds.get(rand1);
                SpatialConcept one = new SpatialConcept(idOne, idsStringMap.get(idOne));
                int idTwo = unknownIds.get(rand2);
                SpatialConcept two = new SpatialConcept(idTwo, idsStringMap.get(idTwo));
                SpatialConceptPair pair = new SpatialConceptPair(one, two);
                if (!allPreviousQList.contains(pair) && !toReturn.contains(pair) && checkScale(one) && checkScale(two)) {
                    setProperties(pair);
                    toReturn.add(pair);
                } else{
                    numbOfPairs++;
                }
            }
        }
        return toReturn;
    }

    public void setProperties(SpatialConceptPair pair) {
        pair.setKmDistance(distanceMatrix[idToIndexForDistanceMatrix.get(pair.getFirstConcept().getUniversalID())][idToIndexForDistanceMatrix.get(pair.getSecondConcept().getUniversalID())]);
        pair.setRelatedness(srMatrix[idToIndexForSRMatrix.get(pair.getFirstConcept().getUniversalID())][idToIndexForSRMatrix.get(pair.getSecondConcept().getUniversalID())]);
        pair.setGraphDistance(graphMatrix[idToIndexForGraphMatrix.get(pair.getFirstConcept().getUniversalID())][idToIndexForGraphMatrix.get(pair.getSecondConcept().getUniversalID())]);
    }
}
