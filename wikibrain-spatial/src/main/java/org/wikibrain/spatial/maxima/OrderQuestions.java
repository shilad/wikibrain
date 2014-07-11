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
        addToReturnList(0, ConceptPairBalancer.chooseOneOffPairs(rw.questions, 9, SemanticRelatednessStraddleStratifier.class), rw); //Picks questions on first page
        addValidationPairs(rw);
        randomFill(rw);
        addDuplicates(rw);
        return rw.toReturn;
    }

    private void addDuplicates(ReturnListWrapper rw) {
        int rand;
        rand = (int) (Math.random() * rw.toReturn[0].size());
        SpatialConceptPair reaskPair = rw.toReturn[0].get(rand);
        rw.toReturn[2].add(reaskPair);
        Collections.shuffle(rw.toReturn[2]);
        SpatialConceptPair reaskPair1;
        SpatialConceptPair reaskPair2;
        do{
            rand = (int) (Math.random() * rw.toReturn[1].size());
            int rand2 =  (int) (Math.random() * rw.toReturn[1].size());
            reaskPair1 = rw.toReturn[1].get(rand);
            reaskPair2 = rw.toReturn[1].get(rand2);
        } while (reaskPair1.equals(reaskPair2) || validationList.contains(reaskPair1) || validationList.contains(reaskPair2));
        rw.toReturn[3].add(reaskPair1);
        rw.toReturn[3].add(reaskPair2);
        Collections.shuffle(rw.toReturn[3]);

    }


    private void randomFill(ReturnListWrapper rw) {
        int rand;
        for (int k = 1; k < 4; k++) {
            int numbOnPage = k == 1 ? 9 : 8;
            for (int i =rw.toReturn[k].size(); i < numbOnPage ; i++) {
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
        validationList.add(new SpatialConceptPair(new SpatialConcept(-1,"Egypt"),new SpatialConcept(-1,"The Nile River")));
        //are these the 4 validation questions we want? TODO
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
            toReturn= new ArrayList[4];
            for(int i = 0; i < toReturn.length; i++) {
                toReturn[i] = new ArrayList<SpatialConceptPair>();
            }
            questions= questionGenerator.getConceptPairsToAsk(knownIds,responseNumb);
        }


    }

}
