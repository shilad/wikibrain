package org.wikibrain.spatial.maxima;

import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.spatial.cookbook.tflevaluate.MatrixReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by harpa003 on 6/27/14.
 * This class will return the questions that will be asked to each person from the getConceptPairsToAsk method
 * uu is a concept pair where the user does not know either concept well spatially
 * ku is a concept pair where the user knows one concept well and not the other
 * kk is a concept pair where the user knows both concepts well spatially
 */

public class SurveyQuestionGenerator {
    private enum KnowledgeType{
        KK,KU,UU
    };
    public static final int RAND_OVER=100; //This is how many more times than the desired number of random concepts will be generated

    public static final int kkMaxTimesAsked=10; //This is the maximum number of times each pair can be asked in given category
    public static final int kuMaxTimesAsked=10;
    public static final int uuMaxTimesAsked=10;

    private HashMap<Integer,String> idsStringMap;
    private Map<Integer,Integer> idToIndexForDistanceMatrix;
    private Map<Integer,Integer> idToIndexForSRMatrix;
    public Map<Integer,Integer> idToIndexForGraphMatrix; //TODO change back to private
    private Map<Integer, Integer> idToScaleCategory;
    MatrixReader matrixReader;
    float[][] distanceMatrix;
    float[][] srMatrix;
    public float[][] graphMatrix; //TODO change back to private

    public List<SpatialConceptPair> allPreviousQList; //the list of previously asked questions and never changes but is added to


    public SurveyQuestionGenerator() {
        allPreviousQList= new ArrayList<SpatialConceptPair>();
        try{
            buildIdsStringMap();
            readInIdToScaleInfo();
        } catch (FileNotFoundException e){
            System.out.println("File not found");
        }
        buildMatrices();
    }

    private void readInIdToScaleInfo() throws FileNotFoundException{
        idToScaleCategory= new HashMap<Integer, Integer>();
        Scanner scanner= new Scanner(new File("geometryToScale.txt"));
        for (int i = 0; i <7 ; i++) { //Throw out the first 7 lines because they are information
            scanner.nextLine();
        }
        while(scanner.hasNextLine()){
            String s= scanner.nextLine();
            String[] info= s.split("\t");
            idToScaleCategory.put(Integer.parseInt(info[0]),Integer.parseInt(info[1]));
        }
    }

    public void test(TIntIntMap map){
        for(Integer id:idsStringMap.keySet()){
            if(!idToIndexForGraphMatrix.containsKey(id)){
                System.out.println(id);
            }
        }
//        for(int id: idToIndexForSRMatrix.keySet()){
//            if(!idsStringMap.containsKey(id)){
//                System.out.println(id);
//            }
//
//        }
//        System.out.println(idToIndexForDistanceMatrix.keySet().size());
//        System.out.println(idsStringMap.keySet().size());
    }

    private void buildMatrices(){
        matrixReader= new MatrixReader();
        MatrixReader.MatrixWithHeader distanceMatrixWithHeader = matrixReader.loadMatrixFile("distancematrix");
        distanceMatrix = distanceMatrixWithHeader.matrix;
        idToIndexForDistanceMatrix= distanceMatrixWithHeader.idToIndex;
        MatrixReader.MatrixWithHeader srMatrixWithHeader = matrixReader.loadMatrixFile("srmatrix");
        srMatrix= srMatrixWithHeader.matrix;
        idToIndexForSRMatrix= srMatrixWithHeader.idToIndex;
        MatrixReader.MatrixWithHeader graphMatrixWithHeader = matrixReader.loadMatrixFile("graphmatrix");
        graphMatrix= graphMatrixWithHeader.matrix;
        idToIndexForGraphMatrix= graphMatrixWithHeader.idToIndex;
    }

    /**
     * Builds the list of universal ids mapped to english titles
     */
    private void buildIdsStringMap() throws FileNotFoundException{
        idsStringMap= new HashMap<Integer, String>();
        File file = new File("IDsToTitles.txt");
        Scanner scanner = new Scanner(file);
        while(scanner.hasNextLine()){
            String next= scanner.nextLine();
            java.util.StringTokenizer st= new java.util.StringTokenizer(next,":",false);
            int id= Integer.parseInt(st.nextToken());
            String name= st.nextToken();
            idsStringMap.put(id, name);
        }
        scanner.close();
    }

    /**
     * Main method that returns a list of concept pairs to ask someone based off of what locations they know spatially
     * @param knownIds
     * @param responseNumb
     * @return
     */
    public List<SpatialConceptPair> getConceptPairsToAsk(List<Integer> knownIds, int responseNumb){
        ArrayList<SpatialConceptPair> returnPairs= new ArrayList<SpatialConceptPair>();
        VariablesWrapper variables= new VariablesWrapper(knownIds,responseNumb);
        GenerateNewSpatialPairs generator = new GenerateNewSpatialPairs(knownIds,idsStringMap,variables.knownIdSet,distanceMatrix,srMatrix,graphMatrix,idToIndexForDistanceMatrix,idToIndexForSRMatrix,idToIndexForGraphMatrix,idToScaleCategory);
        ConceptPairBalancer balancer= new ConceptPairBalancer();

        beccasWay(balancer,variables,generator);
        samsWay(balancer,variables,generator, responseNumb);

        System.out.println(responseNumb + "\tkkReturn List Size: " + variables.kkReturnList.size() + "\tkuReturn List Size: " + variables.kuReturnList.size() + "\tuuReturn List Size: " + variables.uuReturnList.size());
        returnPairs.addAll(variables.kkReturnList);
        returnPairs.addAll(variables.kuReturnList);
        returnPairs.addAll(variables.uuReturnList);
        allPreviousQList.addAll(returnPairs);
        return returnPairs;
    }

    private int calcNewCount(int x) {
        return (int) (Math.log((x + 1400) / 1400) * 1387.7);
    }

    private void samsWay(ConceptPairBalancer balancer, VariablesWrapper variables, GenerateNewSpatialPairs generator, int responseNumber) {
        int newTarget = calcNewCount(responseNumber * 50);


    }

    private void beccasWay(ConceptPairBalancer balancer, VariablesWrapper variables, GenerateNewSpatialPairs generator) {
        getPreviousPairs(balancer, variables);
        getCreatedKkAndKuPairs(generator, balancer, variables);
        getCreatedUUPairs(generator, balancer, (variables.kkReturnList.size() + variables.kuReturnList.size()), variables);
    }


    /**
     * Puts the pair on the appropriate list given its type, increases the number of times it's been asked, and adds it to the previous Q list
     * @param pairList
     * @param type
     */
    private void putOnQuestionsOnList(List<SpatialConceptPair> pairList, KnowledgeType type, VariablesWrapper v) {
        for(SpatialConceptPair pair: pairList) {
            switch (type){
                case KK:
                pair.increaseKkNumbOfTimesAsked(1);
                v.kkReturnList.add(pair);
                    break;
                case KU:
                pair.increaseKuNumbOfTimesAsked(1);
                v.kuReturnList.add(pair);
                    break;
                case UU:
                pair.increaseUuNumbOfTimesAsked(1);
                v.uuReturnList.add(pair);
                    break;
            }
        }
    }


    /**
     * This method puts the ku and kk concept pairs that have been asked previously in the returned lists
     */
    private void getPreviousPairs(ConceptPairBalancer balancer, VariablesWrapper v) {
        putOnQuestionsOnList(balancer.choosePairs(v.kkPreviousQList, v.kkReturnList, v.kkTarget), KnowledgeType.KK, v);
        putOnQuestionsOnList(balancer.choosePairs(v.kuPreviousQList, v.kuReturnList, v.kuTarget),KnowledgeType.KU, v);
    }

    /**
     * Adds to the returned list a list of ku and kk concept pairs that have been newly created and chosen based off of ConceptPairBalancer
     */
    private void getCreatedKkAndKuPairs(GenerateNewSpatialPairs generator, ConceptPairBalancer balancer, VariablesWrapper v) {
        ArrayList<SpatialConceptPair> candidatePairs=generator.kkGenerateSpatialPairs(((v.kkTarget-v.kkReturnList.size())*RAND_OVER),allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidatePairs,v.kkReturnList,(v.kkTarget-v.kkReturnList.size())),KnowledgeType.KK, v);
        candidatePairs=generator.kuGenerateSpatialPairs(((v.kuTarget-v.kuReturnList.size())*RAND_OVER),allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidatePairs, v.kuReturnList, (v.kuTarget - v.kuReturnList.size())), KnowledgeType.KU, v);
    }


    /**
     * Adds to the returned list a list of uu concept pairs both pulled from the previously used questions and newly created
     */
    private void getCreatedUUPairs(GenerateNewSpatialPairs generator, ConceptPairBalancer balancer, int numbOfQuestionsSoFar, VariablesWrapper v) {
        int numbFromOldList = numbOfQuestionsSoFar - v.numbOfNewAllowed;
        int numbRemaining= 50-v.kuReturnList.size()-v.kkReturnList.size();
        putOnQuestionsOnList(balancer.choosePairs(v.uuPreviousQList,v.uuReturnList,numbRemaining),KnowledgeType.UU, v);
        numbRemaining=numbRemaining-v.uuReturnList.size();
        ArrayList<SpatialConceptPair> candidates = generator.uuGenerateSpatialPairs(numbRemaining*RAND_OVER, allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidates,v.uuReturnList,numbRemaining),KnowledgeType.UU, v);
    }


    private class VariablesWrapper{
        public int uuTarget=20; //This is the target number of questions of the uu type returned
        public final int kuTarget=10;
        public int kkTarget=20;
        public List<SpatialConceptPair> uuPreviousQList; //this list changes each time getConceptPairs is called
        public List<SpatialConceptPair> kuPreviousQList;
        public List<SpatialConceptPair> kkPreviousQList;
        public List<SpatialConceptPair> uuReturnList; //this list changes each time getConceptPairs is called
        public List<SpatialConceptPair> kuReturnList;
        public List<SpatialConceptPair> kkReturnList;
        public TIntSet knownIdSet;
        int numbOfNewAllowed;

        VariablesWrapper(List<Integer> knownIds, int responseNumb){
            uuPreviousQList=new ArrayList<SpatialConceptPair>();
            kuPreviousQList=new ArrayList<SpatialConceptPair>();
            kkPreviousQList=new ArrayList<SpatialConceptPair>();
            uuReturnList=new ArrayList<SpatialConceptPair>();
            kuReturnList=new ArrayList<SpatialConceptPair>();
            kkReturnList=new ArrayList<SpatialConceptPair>();
            buildKnownIdSet(knownIds);
            buildPreviousQLists();
            setTargets(responseNumb);
        }

        /**
         * Groups the previously asked questions into kk, ku, and uu based off of the locations the person knows and how many times the question has been asked before
         */
        private void buildPreviousQLists() {
            for(SpatialConceptPair pair:allPreviousQList){
                boolean knowFirstConcept=knownIdSet.contains(pair.getFirstConcept().getUniversalID());
                boolean knowSecondConcept=knownIdSet.contains(pair.getSecondConcept().getUniversalID());
                if(knowFirstConcept){
                    if(knowSecondConcept){
                        putOnPreviousQList(pair,KnowledgeType.KK);
                    } else {
                        putOnPreviousQList(pair,KnowledgeType.KU);
                    }
                } else if(knowSecondConcept){
                    putOnPreviousQList(pair,KnowledgeType.KU);
                } else{
                    putOnPreviousQList(pair,KnowledgeType.UU);
                }
            }
        }

        private void buildKnownIdSet(List<Integer> knownIds){
            knownIdSet= new TIntHashSet();
            knownIdSet.addAll(knownIds);
        }

        private void setTargets(int responseNumb){
            kkTarget=(int) (30-(.02*responseNumb));
            uuTarget=40-kkTarget;
            numbOfNewAllowed= (int) (30-.0008*responseNumb);
        }

        /**
         * This makes sure the question has not been asked more than the allowed time
         * @param pair
         * @param type
         */
        private void putOnPreviousQList(SpatialConceptPair pair, KnowledgeType type){
            switch (type){
                case KK:
                    if(pair.getkkTypeNumbOfTimesAsked()<kkMaxTimesAsked){
                        kkPreviousQList.add(pair);
                    }
                    break;
                case KU:
                    if(pair.getkuTypeNumbOfTimesAsked()<kuMaxTimesAsked){
                        kuPreviousQList.add(pair);
                    }
                    break;
                case UU:
                    if(pair.getuuTypeNumbOfTimesAsked()<uuMaxTimesAsked){
                        uuPreviousQList.add(pair);
                    }
                    break;
            }

        }

    }
}
