package org.wikibrain.spatial.maxima;

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
    public static final int KK=0;
    public static final int KU=1;
    public static final int UU=2;

    public static final int RAND_OVER=100; //This is how many more times than the desired number of random concepts will be generated

    private int uuTarget=20; //This is the target number of questions of the uu type returned
    private final int kuTarget=10;
    private int kkTarget=20;
    public static final int kkMaxTimesAsked=15; //This is the maximum number of times each pair can be asked in given category
    public static final int kuMaxTimesAsked=15;
    public static final int uuMaxTimesAsked=15;

    private HashMap<Integer,String> idsStringMap;
    private Map<Integer,Integer> idToIndexForDistanceMatrix;
    private TIntSet knownIdSet;
    MatrixReader matrixReader;
    float[][] distanceMatrix;
    float[][] srMatrix;
    float[][] graphMatrix;

    public List<SpatialConceptPair> allPreviousQList; //the list of previously asked questions and never changes but is added to
    private List<SpatialConceptPair> uuPreviousQList; //this list changes each time getConceptPairs is called
    private List<SpatialConceptPair> kuPreviousQList;
    private List<SpatialConceptPair> kkPreviousQList;
    private List<SpatialConceptPair> uuReturnList; //this list changes each time getConceptPairs is called
    private List<SpatialConceptPair> kuReturnList;
    private List<SpatialConceptPair> kkReturnList;

    public SurveyQuestionGenerator() {
        allPreviousQList= new ArrayList<SpatialConceptPair>();
        matrixReader= new MatrixReader();
        try{
            buildIdsStringMap();
        } catch (FileNotFoundException e){
            System.out.println("File not found");
        }
        MatrixReader.MatrixWithHeader distanceMatrixWithHeader = matrixReader.loadMatrixFile("distancematrix");
        distanceMatrix = distanceMatrixWithHeader.matrix;
        MatrixReader.MatrixWithHeader srMatrixWithHeader = matrixReader.loadMatrixFile("srmatrix");
        srMatrix= srMatrixWithHeader.matrix;
        MatrixReader.MatrixWithHeader graphMatrixWithHeader = matrixReader.loadMatrixFile("graphmatrix");
        graphMatrix= graphMatrixWithHeader.matrix;
        idToIndexForDistanceMatrix= distanceMatrixWithHeader.idToIndex;
    }

    /**
     * Builds the list of universal ids mapped to english titles
     */
    public void buildIdsStringMap() throws FileNotFoundException{
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
        buildKnownIdSet(knownIds);
        GenerateNewSpatialPairs generator = new GenerateNewSpatialPairs(knownIds,idsStringMap,knownIdSet,idToIndexForDistanceMatrix,distanceMatrix,srMatrix,graphMatrix);
        ConceptPairBalancer balancer= new ConceptPairBalancer();
        kkReturnList= new ArrayList<SpatialConceptPair>();
        kuReturnList= new ArrayList<SpatialConceptPair>();
        uuReturnList= new ArrayList<SpatialConceptPair>();
        uuPreviousQList= new ArrayList<SpatialConceptPair>();
        kuPreviousQList= new ArrayList<SpatialConceptPair>();
        kkPreviousQList= new ArrayList<SpatialConceptPair>();
        buildPreviousQLists();
        int nunbOfNewAllowed=setTargets(responseNumb); //based on responseNumber and curve TODO:

        getPreviousPairs(balancer);
        getCreatedKkAndKuPairs(generator,balancer);
        getCreatedUUPairs(generator, balancer, (kkReturnList.size() + kuReturnList.size()), nunbOfNewAllowed);

        returnPairs.addAll(kkReturnList);
        returnPairs.addAll(kuReturnList);
        returnPairs.addAll(uuReturnList);
        return returnPairs;
    }

    private int setTargets(int responseNumb){
        setKkTarget((int) (30-(.0004*responseNumb)));
        setUuTarget(40-kkTarget);
        return (int) (50-.0008*responseNumb);
    }

    private void buildKnownIdSet(List<Integer> knownIds){
        knownIdSet= new TIntHashSet();
        knownIdSet.addAll(knownIds);
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
                    putOnPreviousQList(pair,KK);
                } else {
                    putOnPreviousQList(pair,KU);
                }
            } else if(knowSecondConcept){
                putOnPreviousQList(pair,KU);
            } else{
                putOnPreviousQList(pair,UU);
            }
        }
    }

    /**
     * This makes sure the question has not been asked more than the allowed time
     * @param pair
     * @param type
     */
    private void putOnPreviousQList(SpatialConceptPair pair, int type){
        if(type==KK && pair.getkkTypeNumbOfTimesAsked()<kkMaxTimesAsked){
            kkPreviousQList.add(pair);
        } else if(type==KU && pair.getkuTypeNumbOfTimesAsked()<kuMaxTimesAsked){
            kuPreviousQList.add(pair);
        } else if(type==UU && pair.getuuTypeNumbOfTimesAsked()<uuMaxTimesAsked){
            uuPreviousQList.add(pair);
        }
    }



    /**
     * Puts the pair on the appropriate list given its type, increases the number of times it's been asked, and adds it to the previous Q list
     * @param pairList
     * @param type
     */
    private void putOnQuestionsOnList(List<SpatialConceptPair> pairList, int type) {
        for(SpatialConceptPair pair: pairList) {
            if (type == KK) {
                pair.increaseKkNumbOfTimesAsked(1);
                kkReturnList.add(pair);
            } else if (type == KU) {
                pair.increaseKuNumbOfTimesAsked(1);
                kuReturnList.add(pair);
            } else if (type == UU) {
                pair.increaseUuNumbOfTimesAsked(1);
                uuReturnList.add(pair);
            }
            allPreviousQList.add(pair);
        }
    }


    /**
     * This method puts the ku and kk concept pairs that have been asked previously in the returned lists
     */
    private void getPreviousPairs(ConceptPairBalancer balancer) {
        putOnQuestionsOnList(balancer.choosePairs(kkPreviousQList, kkReturnList, kkTarget), KK);
        putOnQuestionsOnList(balancer.choosePairs(kuPreviousQList,kuReturnList,kuTarget),KU);
    }

    /**
     * Adds to the returned list a list of ku and kk concept pairs that have been newly created and chosen based off of ConceptPairBalancer
     */
    private void getCreatedKkAndKuPairs(GenerateNewSpatialPairs generator, ConceptPairBalancer balancer) {
        ArrayList<SpatialConceptPair> candidatePairs=generator.kkGenerateSpatialPairs(((kkTarget-kkReturnList.size())*RAND_OVER),allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidatePairs,kkReturnList,(kkTarget-kkReturnList.size())),KK);
        candidatePairs=generator.kuGenerateSpatialPairs(((kuTarget-kuReturnList.size())*RAND_OVER),allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidatePairs,kuReturnList,(kuTarget-kuReturnList.size())),KU);
    }


    /**
     * Adds to the returned list a list of uu concept pairs both pulled from the previously used questions and newly created
     * @param numbOfQuestionsSoFar
     * @param numbOfNewQuestionsAllowed
     */
    private void getCreatedUUPairs(GenerateNewSpatialPairs generator, ConceptPairBalancer balancer, int numbOfQuestionsSoFar, int numbOfNewQuestionsAllowed) {
        int numbFromOldList = numbOfQuestionsSoFar - numbOfNewQuestionsAllowed;
        putOnQuestionsOnList(balancer.choosePairs(uuPreviousQList,uuReturnList,numbFromOldList),UU);
        ArrayList<SpatialConceptPair> canidates = generator.uuGenerateSpatialPairs(numbOfNewQuestionsAllowed, allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(canidates,uuReturnList,numbOfNewQuestionsAllowed),UU);
    }


    public void setUuTarget(int uuTarget) {
        this.uuTarget = uuTarget;
    }

    public void setKkTarget(int kkTarget) {
        this.kkTarget = kkTarget;
    }
}
