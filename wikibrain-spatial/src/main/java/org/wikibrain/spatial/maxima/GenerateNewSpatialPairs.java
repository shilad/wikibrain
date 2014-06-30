package org.wikibrain.spatial.maxima;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.spatial.cookbook.tflevaluate.InstanceOfExtractor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by harpa003 on 6/27/14.
 */
public class GenerateNewSpatialPairs {

    private List<Integer> knownIds;
    private ArrayList<Integer> unknownIds;
    HashMap<Integer,String> idsStringMap;
    private Map<Integer,Integer> idToIndexForMatrices;
    private float[][] distanceMatrix;
    private float[][] srMatrix;
    private float[][] graphMatrix;



    public GenerateNewSpatialPairs(List<Integer> knownIdList, HashMap<Integer, String> idsStringMap, TIntSet knownIdSet, Map<Integer, Integer> idToIndexForMatrices, float[][] distanceMatrix, float[][] srMatrix, float[][] graphMatrix) {
        knownIds= knownIdList;
        unknownIds= new ArrayList<Integer>();
        this.idsStringMap=idsStringMap;
        buildUnknownIdList(knownIdSet,idsStringMap);
        this.idToIndexForMatrices=idToIndexForMatrices;
        this.distanceMatrix= distanceMatrix;
        this.srMatrix= srMatrix;
    }

    private void buildUnknownIdList(TIntSet knownIdSet, HashMap<Integer,String> idsStringMap) {
        for(Integer id:idsStringMap.keySet()){
            if(!knownIdSet.contains(id)){
                unknownIds.add(id);
            }
        }
    }

    public ArrayList<SpatialConceptPair> kkGenerateSpatialPairs(int numbOfPairs, List<SpatialConceptPair> allPreviousQList){
        ArrayList<SpatialConceptPair> toReturn= new ArrayList<SpatialConceptPair>();
        for (int i = 0; i < numbOfPairs ; i++) {
            int rand1= (int) (Math.random()*knownIds.size());
            int rand2= (int) (Math.random()*knownIds.size());
            if(rand1 !=rand2) {
                int idOne=knownIds.get(rand1);
                SpatialConcept one= new SpatialConcept(idOne,idsStringMap.get(idOne));
                int idTwo= knownIds.get(rand2);
                SpatialConcept two= new SpatialConcept(idTwo,idsStringMap.get(idTwo));
                SpatialConceptPair pair= new SpatialConceptPair(one,two);
                if(!allPreviousQList.contains(pair)) {
                    setProperties(pair);
                    toReturn.add(pair);
                }
            } else{
                numbOfPairs++;
            }
        }
        return toReturn;
    }

    public ArrayList<SpatialConceptPair> kuGenerateSpatialPairs(int numbOfPairs, List<SpatialConceptPair> allPreviousQList){
        ArrayList<SpatialConceptPair> toReturn= new ArrayList<SpatialConceptPair>();
        for (int i = 0; i < numbOfPairs ; i++) {
            int rand1= (int) (Math.random()*knownIds.size());
            int rand2= (int) (Math.random()*unknownIds.size());
            int idOne=knownIds.get(rand1);
            SpatialConcept one= new SpatialConcept(idOne,idsStringMap.get(idOne));
            int idTwo= unknownIds.get(rand2);
            SpatialConcept two= new SpatialConcept(idTwo,idsStringMap.get(idTwo));
            SpatialConceptPair pair= new SpatialConceptPair(one,two);
            if(!allPreviousQList.contains(pair)) {
                setProperties(pair);
                toReturn.add(pair);
            }
        }
        return toReturn;
    }

    public ArrayList<SpatialConceptPair> uuGenerateSpatialPairs(int numbOfPairs, List<SpatialConceptPair> allPreviousQList){
        ArrayList<SpatialConceptPair> toReturn= new ArrayList<SpatialConceptPair>();
        for (int i = 0; i < numbOfPairs ; i++) {
            int rand1= (int) (Math.random()*unknownIds.size());
            int rand2= (int) (Math.random()*unknownIds.size());
            int idOne=unknownIds.get(rand1);
            SpatialConcept one= new SpatialConcept(idOne,idsStringMap.get(idOne));
            int idTwo= unknownIds.get(rand2);
            SpatialConcept two= new SpatialConcept(idTwo,idsStringMap.get(idTwo));
            SpatialConceptPair pair= new SpatialConceptPair(one,two);
            if(!allPreviousQList.contains(pair)) { //TODO this will always return true
                setProperties(pair);
                toReturn.add(pair);
            }
        }
        return toReturn;
    }

    public void setProperties(SpatialConceptPair pair) {
        pair.setKmDistance(distanceMatrix[idToIndexForMatrices.get(pair.getFirstConcept().getUniversalID())][idToIndexForMatrices.get(pair.getSecondConcept().getUniversalID())]);
        pair.setRelatedness(srMatrix[idToIndexForMatrices.get(pair.getFirstConcept().getUniversalID())][idToIndexForMatrices.get(pair.getSecondConcept().getUniversalID())]);
        pair.setGraphDistance(graphMatrix[idToIndexForMatrices.get(pair.getFirstConcept().getUniversalID())][idToIndexForMatrices.get(pair.getSecondConcept().getUniversalID())]);
    }
}
