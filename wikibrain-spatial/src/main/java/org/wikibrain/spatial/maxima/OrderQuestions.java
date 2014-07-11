package org.wikibrain.spatial.maxima;

import java.util.*;

/**
 * Created by harpa003 on 7/3/14.
 */
public class OrderQuestions {
    Set<SpatialConceptPair> validationList;
    SurveyQuestionGenerator questionGenerator;

    public OrderQuestions(){
        initValidationList();
        questionGenerator= new SurveyQuestionGenerator();
    }

    public List<SpatialConceptPair>[] getQuestions(List<Integer> knownIds, int responseNumb){
        ReturnListWrapper rw= new ReturnListWrapper(knownIds,responseNumb);
        addToReturnList(0, ConceptPairBalancer.chooseOneOffPairs(rw.questions, 10, SemanticRelatednessStratifier.class), rw);
        addValidationPairs(rw);
        randomFill(rw);
        addDuplicates(rw);
        return rw.toReturn;
    }

    private void addDuplicates(ReturnListWrapper rw) {
        int rand;
        for (int i = 0; i < 3; i++) {
            SpatialConceptPair reaskPair = null;
            do {
                rand = (int) (Math.random() * rw.toReturn[i].size());
                reaskPair = rw.toReturn[i].get(rand);
            } while (validationList.contains(reaskPair));
            rw.toReturn[i + 2].add(reaskPair);
            Collections.shuffle(rw.toReturn[i + 2]);
        }

    }


    private void randomFill(ReturnListWrapper rw) {
        int rand;
        for (int k = 1; k < 5; k++) {
            for (int i =rw.toReturn[k].size(); i < 9 ; i++) {
                if(!rw.questions.isEmpty()){
                    rand=(int) (Math.random()*rw.questions.size());
                    addToReturnList(k, rw.questions.get(rand), rw);
                }
            }
        }
    }


    private void addValidationPairs(ReturnListWrapper rw) {
        rw.questions.addAll(validationList);
    }

    private void initValidationList(){
        validationList= new HashSet<SpatialConceptPair>();
        validationList.add(new SpatialConceptPair(new SpatialConcept(-1,"USA"),new SpatialConcept(-1,"America")));
        validationList.add(new SpatialConceptPair(new SpatialConcept(-1,"Sydney Opera House"),new SpatialConcept(-1,"Australia")));
        validationList.add(new SpatialConceptPair(new SpatialConcept(-1,"Minnesota"),new SpatialConcept(-1,"Eiffel Tower")));
        validationList.add(new SpatialConceptPair(new SpatialConcept(-1,"India"),new SpatialConcept(-1,"The Nile River")));
        //are these the 4 validation questions we want?
    }

    public void addToReturnList(int pageToGoOn, List<SpatialConceptPair> questionsForPg, ReturnListWrapper rw){
        rw.toReturn[pageToGoOn].addAll(questionsForPg);
        rw.questions.removeAll(questionsForPg);
    }
    public void addToReturnList(int pageToGoOn, SpatialConceptPair pairToAdd, ReturnListWrapper rw){
        rw.toReturn[pageToGoOn].add(pairToAdd);
        rw.questions.remove(pairToAdd);
    }

    private class ReturnListWrapper{
        List<SpatialConceptPair> toReturn[];
        List<SpatialConceptPair> questions;

        public ReturnListWrapper(List<Integer> knownIds, int responseNumb){
            toReturn= new ArrayList[5];
            for(int i = 0; i < 5; i++) {
                toReturn[i] = new ArrayList<SpatialConceptPair>();
            }
            questions= questionGenerator.getConceptPairsToAsk(knownIds,responseNumb);
        }


    }

}
