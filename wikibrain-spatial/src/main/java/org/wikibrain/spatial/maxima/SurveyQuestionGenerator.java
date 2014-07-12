package org.wikibrain.spatial.maxima;

import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.spatial.cookbook.tflevaluate.MatrixGenerator;


import java.io.*;
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
    public static final int RAND_OVER=1000; //This is how many more times than the desired number of random concepts will be generated

    public static final int kkMaxTimesAsked=15; //This is the maximum number of times each pair can be asked in given category
    public static final int kuMaxTimesAsked=10;
    public static final int uuMaxTimesAsked=15;

    private HashMap<Integer,String> idsStringMap;
    private Map<Integer,Integer> idToIndexForDistanceMatrix;
    private Map<Integer,Integer> idToIndexForSRMatrix;
    public Map<Integer,Integer> idToIndexForGraphMatrix; //TODO change back to private
    private Map<Integer, Integer> idToScaleCategory;
    float[][] distanceMatrix;
    float[][] srMatrix;
    public float[][] graphMatrix; //TODO change back to private
    PrintWriter pw;

    public Set<SpatialConceptPair> allPreviousQList;
//    public List<SpatialConceptPair> allPreviousQList; //the list of previously asked questions and never changes but is added to


    public SurveyQuestionGenerator(){
        allPreviousQList= new HashSet<SpatialConceptPair>();
//        allPreviousQList= new ArrayList<SpatialConceptPair>();
        try{
            buildIdsStringMap();
            readInIdToScaleInfo();
        } catch (FileNotFoundException e){
            System.out.println("File not found");
        }
        buildMatrices();
        try {
            resetPreviousQList();
            pw = new PrintWriter(new FileWriter("previousConceptPairs.txt", true)); //sets autoflush to true
            pw.flush();
        }catch(IOException e){
            System.out.println("IOException");
        }
    }

    /**
     * rebuilds the allPreviousQList Set in the event of survey crash
     * @throws IOException
     */
    private void resetPreviousQList() throws IOException{
        Scanner sc = new Scanner(new File("previousConceptPairs.txt"));
        List<SpatialConceptPair> previousQList = new ArrayList<SpatialConceptPair>();

        while(sc.hasNextLine()){
            String[] info = sc.nextLine().split(":");
            int firstConceptID = Integer.parseInt(info[0]);
            int secondConceptID = Integer.parseInt(info[1]);
            SpatialConcept firstConcept = new SpatialConcept(firstConceptID, idsStringMap.get(firstConceptID));
            assignScale(firstConcept);
            SpatialConcept secondConcept = new SpatialConcept(secondConceptID, idsStringMap.get(secondConceptID));
            assignScale(secondConcept);
            SpatialConceptPair pair = new SpatialConceptPair(firstConcept, secondConcept);

            int listIndex = previousQList.indexOf(pair);//finds the index of the pair
            if (listIndex == -1){//adds the pair if not in the list
                previousQList.add(pair);
                listIndex = previousQList.size()-1; //changes listIndex to the index of the new pair for the next part
            }

            String type = info[2];
            //increases the proper number of times asked for the line read in
            if(type.equals("kk")){
                previousQList.get(listIndex).increaseKkNumbOfTimesAsked(1);
            }
            else if(type.equals("ku")){
                previousQList.get(listIndex).increaseKuNumbOfTimesAsked(1);
            }
            else{
                previousQList.get(listIndex).increaseUuNumbOfTimesAsked(1);
            }
        }

        sc.close();
        allPreviousQList.addAll(previousQList);
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


    private void buildMatrices(){
        MatrixGenerator.MatrixWithHeader distanceMatrixWithHeader = MatrixGenerator.loadMatrixFile("distancematrix_en");
        distanceMatrix = distanceMatrixWithHeader.matrix;
        idToIndexForDistanceMatrix= distanceMatrixWithHeader.idToIndex;
        MatrixGenerator.MatrixWithHeader srMatrixWithHeader = MatrixGenerator.loadMatrixFile("srmatrix_en");
        srMatrix= srMatrixWithHeader.matrix;
        idToIndexForSRMatrix= srMatrixWithHeader.idToIndex;
        MatrixGenerator.MatrixWithHeader graphMatrixWithHeader = MatrixGenerator.loadMatrixFile("graphmatrix_en");
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
            java.util.StringTokenizer st= new java.util.StringTokenizer(next,"\t",false);
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

        getPreviousPairs(balancer, variables);
        getCreatedKkAndKuPairs(generator, balancer, variables);
        getCreatedUUPairs(generator, balancer, (variables.kkReturnList.size() + variables.kuReturnList.size()), variables);

//        System.out.println(responseNumb + "\tkkReturn List Size: " + variables.kkReturnList.size() + "\tkuReturn List Size: " + variables.kuReturnList.size() + "\tuuReturn List Size: " + variables.uuReturnList.size());
        returnPairs.addAll(variables.kkReturnList);
        returnPairs.addAll(variables.kuReturnList);
        returnPairs.addAll(variables.uuReturnList);
        allPreviousQList.addAll(returnPairs);
        if(responseNumb % 100==0){
            System.out.println(responseNumb);
        }

        synchronized (pw) {
            //appends all information for each pair being asked into previousConceptPairs.txt in the event of a survey crash
            for(SpatialConceptPair pair: variables.kkReturnList){
                pw.append(pair.getFirstConcept().getUniversalID() + ":" + pair.getSecondConcept().getUniversalID() + ":" + "kk\n");
            }

            for(SpatialConceptPair pair: variables.kuReturnList){
                pw.append(pair.getFirstConcept().getUniversalID() + ":" + pair.getSecondConcept().getUniversalID() + ":" + "ku\n");
            }

            for(SpatialConceptPair pair: variables.uuReturnList){
                pw.append(pair.getFirstConcept().getUniversalID() + ":" + pair.getSecondConcept().getUniversalID() + ":" + "uu\n");
            }
            pw.flush();
        }

        return returnPairs;
    }

    private void assignScale(SpatialConcept concept) {
        Integer scale= idToScaleCategory.get(concept.getUniversalID());

        if(scale==1){
            concept.setScale(SpatialConcept.Scale.LANDMARK);
        } else if(scale==3){
            concept.setScale(SpatialConcept.Scale.COUNTRY);
        } else if(scale==4){
            concept.setScale(SpatialConcept.Scale.STATE);
        } else if(scale==5){
            concept.setScale(SpatialConcept.Scale.CITY);
        }else if(scale==6){
            concept.setScale(SpatialConcept.Scale.NATURAL);
        }

    }

    /**
     * Puts the pair on the appropriate list given its type, increases the number of times it's been asked, and adds it to the previous Q list
     * @param pairList
     * @param type
     */
    private void putOnQuestionsOnList(Set<SpatialConceptPair> pairList, KnowledgeType type, VariablesWrapper v) {
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
        putOnQuestionsOnList(balancer.choosePairs(v.kuPreviousQList, v.kuReturnList, v.kuTarget), KnowledgeType.KU, v);
    }

    /**
     * Adds to the returned list a list of ku and kk concept pairs that have been newly created and chosen based off of ConceptPairBalancer
     */
    private void getCreatedKkAndKuPairs(GenerateNewSpatialPairs generator, ConceptPairBalancer balancer, VariablesWrapper v) {
        Set<SpatialConceptPair> candidatePairs=generator.kkGenerateSpatialPairs(allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidatePairs, v.kkReturnList, (v.kkTarget - v.kkReturnList.size())), KnowledgeType.KK, v);
        candidatePairs=generator.kuGenerateSpatialPairs(((v.kuTarget-v.kuReturnList.size())*RAND_OVER),allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidatePairs, v.kuReturnList, (v.kuTarget - v.kuReturnList.size())), KnowledgeType.KU, v);
    }


    /**
     * Adds to the returned list a list of uu concept pairs both pulled from the previously used questions and newly created
     */
    private void getCreatedUUPairs(GenerateNewSpatialPairs generator, ConceptPairBalancer balancer, int numbOfQuestionsSoFar, VariablesWrapper v) {
        int numbRemaining= v.kkTarget+v.uuTarget+v.kuTarget-v.kuReturnList.size()-v.kkReturnList.size();
        putOnQuestionsOnList(balancer.choosePairs(v.uuPreviousQList,v.uuReturnList,numbRemaining),KnowledgeType.UU, v);
        numbRemaining=numbRemaining-v.uuReturnList.size();
        Set<SpatialConceptPair> candidates = generator.uuGenerateSpatialPairs(numbRemaining*RAND_OVER*10, allPreviousQList);
        putOnQuestionsOnList(balancer.choosePairs(candidates,v.uuReturnList,numbRemaining),KnowledgeType.UU, v);
    }


    private class VariablesWrapper{
        public int uuTarget; //This is the target number of questions of the uu type returned
        public final int kuTarget=7;
        public int kkTarget;
        public Set<SpatialConceptPair> uuPreviousQList; //this list changes each time getConceptPairs is called
        public Set<SpatialConceptPair> kuPreviousQList;
        public Set<SpatialConceptPair> kkPreviousQList;
        public Set<SpatialConceptPair> uuReturnList; //this list changes each time getConceptPairs is called
        public Set<SpatialConceptPair> kuReturnList;
        public Set<SpatialConceptPair> kkReturnList;
        public TIntSet knownIdSet;

        VariablesWrapper(List<Integer> knownIds, int responseNumb){
            uuPreviousQList=new HashSet<SpatialConceptPair>();
            kuPreviousQList=new HashSet<SpatialConceptPair>();
            kkPreviousQList=new HashSet<SpatialConceptPair>();
            uuReturnList=new HashSet<SpatialConceptPair>();
            kuReturnList=new HashSet<SpatialConceptPair>();
            kkReturnList=new HashSet<SpatialConceptPair>();
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
            kkTarget=(int) (15-(.007*responseNumb));
            uuTarget=23-kkTarget;
//            kkTarget=(int) (20-(.01*responseNumb));
//            uuTarget=30-kkTarget;
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
