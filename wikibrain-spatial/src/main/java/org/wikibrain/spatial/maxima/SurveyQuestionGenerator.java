package org.wikibrain.spatial.maxima;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by harpa003 on 6/27/14.
 * This class will return the questions that will be asked to each person
 * uu is a concept pair where the user does not know either concept well spatially
 * ku is a concept pair where the user knows one concept well and not the other
 * kk is a concept pair where the user knows both concepts well spatially
 */
public class SurveyQuestionGenerator {
    int uuTarget=20; //This is the target number of questions of the uu type returned
    int kuTarget=20;
    int kkTarget=20;
    List<SpatialConceptPair> previousQList; //the list of previously asked questions

    public SurveyQuestionGenerator() {
    }

    public List<SpatialConceptPair> getConceptPairsToAsk(List<Integer> knownIds, int responseNumb){
        TIntSet knownIdSet= new TIntHashSet();
        for(Integer id:knownIds){
            knownIdSet.add(id);
        }
        List<SpatialConceptPair> pairs= new ArrayList<SpatialConceptPair>();
        int nunbOfNewAllowed;
        for(SpatialConceptPair sp: getPreviousPairs(knownIdSet)){
            pairs.add(sp);
        }for(SpatialConceptPair sp: getCreatedPairs(knownIdsSet)){
            pairs.add(sp);
        }for(SpatialConceptPair sp: getCreatedUUPairs(knownIdsSet,pairs.size(),nunbOfNewAllowed)){
            pairs.add(sp);
        }
        return pairs;
    }

    /**
     * Returns a list of ku and kk concept pairs that have been newly created based
     * @param knownIds
     * @return
     */
    private List<SpatialConceptPair> getCreatedPairs(List<Integer> knownIds) {
    }

    /**
     * This method returns the ku and kk concept pairs that have been asked previously
     * @param knownIdSet
     * @return
     */
    private List<SpatialConceptPair> getPreviousPairs(TIntSet knownIdSet) {
        List<SpatialConceptPair> toReturn= new ArrayList<SpatialConceptPair>();
        List<SpatialConceptPair> kkList= new ArrayList<SpatialConceptPair>();
        List<SpatialConceptPair> ku= new ArrayList<SpatialConceptPair>();
        for(SpatialConceptPair pair:previousQList){
            if(knownIdSet.contains(pair.getFirstConcept().getUniversalID()) || knownIdSet.contains(pair.getSecondConcept().getUniversalID())){

            }
        }
    }

    /**
     * Returns a list of uu concept pairs both pulled from the previously used questions and newly created
     * @param knownIds
     * @param numbOfQuestionsSoFar
     * @param numbOfNewQuestionsAllowed
     * @return
     */
    private List<SpatialConceptPair> getCreatedUUPairs(List<Integer> knownIds, int numbOfQuestionsSoFar, int numbOfNewQuestionsAllowed) {
    }


    public void setUuTarget(int uuTarget) {
        this.uuTarget = uuTarget;
    }

    public void setKuTarget(int kuTarget) {
        this.kuTarget = kuTarget;
    }

    public void setKkTarget(int kkTarget) {
        this.kkTarget = kkTarget;
    }
}
