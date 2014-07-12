package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.maxima.OrderQuestions;
import org.wikibrain.spatial.maxima.SpatialConcept;
import org.wikibrain.spatial.maxima.SpatialConceptPair;
import org.wikibrain.spatial.maxima.SurveyQuestionGenerator;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Created by harpa003 on 7/11/14.
 */
    public class SimulatedTurkers2 {

        private Map<String, Double> cityPopulations;
        private Map<String, Geometry> cityGeometries;

        boolean TurkersWithKnownIDs=true;
        private Map<Integer,String> idToStringMap;


        public SimulatedTurkers2() throws ConfigurationException, DaoException {

            try{
                buildIDtoStringMap();
            } catch (Exception e){}
        }

    private void buildIDtoStringMap() throws FileNotFoundException{
        idToStringMap= new HashMap<Integer, String>();
        File file = new File("IDsToTitles.txt");
        Scanner scanner = new Scanner(file);
        while(scanner.hasNextLine()){
            String next= scanner.nextLine();
            java.util.StringTokenizer st= new java.util.StringTokenizer(next,"\t",false);
            int id= Integer.parseInt(st.nextToken());
            String name= st.nextToken();
            idToStringMap.put(id, name);
        }
        scanner.close();
    }

    public static void main(String[] args) throws ConfigurationException, DaoException {
           SimulatedTurkers2 st = new SimulatedTurkers2();
            Map<String, Set<Integer>> neighbors = st.loadNeighborFile(new File("citiesToNeighbors4.txt"));
//
            //set number of simulated turkers to 1000
            List<Set<Integer>> simulatedTurkers = st.generateSimulatedTurkers(neighbors, st.cityPopulations, 1000);

            SurveyQuestionGenerator generator = new SurveyQuestionGenerator();
            OrderQuestions orderer= new OrderQuestions();

            List<Integer> knownIds = new ArrayList<Integer>();


            PrintWriter pwt=null;
            PrintWriter pw2=null;
            try{
                pwt= new PrintWriter(new FileWriter("BeccasTest.txt"));
                pw2= new PrintWriter(new FileWriter("TurkReorderedResult.txt"));
            }catch (Exception e){
                System.out.println("File not found");
            }
            int notR;
            int sortaR;
            int VeryR;

            //FOR EACH TURKER LOOP
            for (int i = 0; i < simulatedTurkers.size(); i++) {

                pwt.println(i+"\t"+st.idToStringMap.get(simulatedTurkers.get(i).toArray()[0]));
                pw2.println(i+"\t"+st.idToStringMap.get(simulatedTurkers.get(i).toArray()[0])+"---------------------------------------------------------------------------------");

                knownIds.clear();
                knownIds.addAll(simulatedTurkers.get(i));
                List<SpatialConceptPair> questions = generator.getConceptPairsToAsk(knownIds, i);
                List<SpatialConceptPair>[] reOrderedQ = orderer.getQuestions(knownIds,i);

                int numb=1;
                notR=0;
                sortaR=0;
                VeryR=0;
                for (SpatialConceptPair pair : questions) {
                    pwt.println("\t"+numb+"\tType: "+getType(pair, knownIds)+"\t"+pair);
                    numb++;
                    if(pair.getRelatedness()<.3){
                        notR++;
                    }else if(pair.getRelatedness()<.65){
                        sortaR++;
                    } else VeryR++;
                }
                pwt.println("\t\t\tRelated Column: Not Related: "+notR+"\t\tSorta Related: "+sortaR+"\t\tVery Related: "+VeryR);

                numb=1;
                List<SpatialConceptPair> prevQ= new ArrayList<SpatialConceptPair>();
                Set<SpatialConcept> noSRList= new HashSet<SpatialConcept>();

                for (int j = 0; j < reOrderedQ.length; j++) {
                    notR=0;
                    sortaR=0;
                    VeryR=0;
                    pw2.println("\tPage "+j);
                    for(SpatialConceptPair pair: reOrderedQ[j]){
                        if(pair.getRelatedness()<.3){
                            notR++;
                        }else if(pair.getRelatedness()<.65){
                            sortaR++;
                        } else VeryR++;

                        if(pair.getFirstConcept().getUniversalID() < 0){
                            pw2.print("*");
                        }
                        else if(prevQ.contains(pair)){
                            pw2.print("~");
                        }

                        pw2.println("\t\t" + numb+"\tType: "+getType(pair, knownIds)+"\tSR: "+pair.getRelatedness()+"\tKMDist: "+pair.getKmDistance()+"\tGraphD: "+pair.getGraphDistance()+"\t"+pair);
                        prevQ.add(pair);
                        numb++;
                    }pw2.println("\t\t\t\t\t\t\t\t\t\t\tRelated Column: Not Related: "+notR+"\tSorta Related: "+sortaR+"\tVery Related: "+VeryR);
                }
                checkForDUPLICATES(questions);

            }
            pwt.close();
            pw2.close();
            int count = 0;
            int notuucount = 0;
            try {
                PrintWriter wt = new PrintWriter(new FileWriter("TurkResult.txt"));
                for (SpatialConceptPair pair : generator.allPreviousQList) {
                    if (pair.getkkTypeNumbOfTimesAsked() > 9) {
                        wt.println("KKTimes: " + pair.getkkTypeNumbOfTimesAsked() + "\tUUTimes: " + pair.getuuTypeNumbOfTimesAsked() + "\t" + pair.getFirstConcept().getTitle() + "\t" + pair.getSecondConcept().getTitle());
                        count++;
                        if (pair.getuuTypeNumbOfTimesAsked() < 10) {
                            notuucount++;
                        }

                    }
                }
                wt.close();
            }
            catch (Exception e){
                System.out.println("File not found");
            }


            System.out.println("Number of KK QuestionPairs " + count);
            System.out.println("Number of not enough uu QuestionPairs " + notuucount);
            System.out.println("Number of overall question pairs " + generator.allPreviousQList.size());
//            printNumberOfPairsDistributionForKKandUU(generator);


        }

        private static void checkForDUPLICATES(List<SpatialConceptPair> questions) {
            int duplicates=0;
            for(SpatialConceptPair pair:questions){
                for(SpatialConceptPair pair2: questions) {
                    if (pair.equals(pair2)) {
                        duplicates++;
                    }
                }
            }
            if(duplicates>questions.size()){
                System.out.println("OHHH NO THERE ARE DUPLICATES!!!!!!-------------------------------------------");
            }
        }

        private static void printNumberOfPairsDistributionForKKandUU(SurveyQuestionGenerator generator) {
            int[][] becca= new int[16][16];
            for (int i = 0; i < 16; i++) {
                for (SpatialConceptPair pair : generator.allPreviousQList) {
                    if (pair.getkkTypeNumbOfTimesAsked() == i){
                        for (int j = 0; j <16 ; j++) {
                            if(pair.getuuTypeNumbOfTimesAsked()==j){
                                becca[i][j]++;
                            }
                        }
                    }
                }
            }
            for (int i =01; i < 16; i++) {
                for (int j = 0; j <16 ; j++) {
                    System.out.println("KK Times "+ i+"\tUU Times "+ j+" Number of Pairs "+becca[i][j]);
                }
            }
        }

        private static String getType(SpatialConceptPair pair, List<Integer> knownIds) {
            if(knownIds.contains(pair.getFirstConcept().getUniversalID())){
                if(knownIds.contains(pair.getSecondConcept().getUniversalID())){
                    return "KK";
                }
                return "KU";
            } return "UU";
        }

        /**
         * Create a plain-text file whose format consists of the (tab separated) fields
         * city_country,city_state,city_name    city_population  city_neighbor_1  city_neighbor_2  ...
         * for each city
         *
         * @param map      Map from city id (city_country,city_state,city_name) to city neighbors
         * @param filename The filename to store this file in
         */
        public void createNeighborFile(Map<String, Set<Integer>> map, String filename) {

            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(filename));

                for (String string : map.keySet()) {
                    bw.write(string + "\t" + cityPopulations.get(string) + "\t");
                    for (Integer i : map.get(string)) {
                        bw.write(i + "\t");
                    }
                    bw.write("\n");
                }
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Read in a plain-text file whose format consists of the (tab separated) fields
         * city_country,city_state,city_name    city_population  city_neighbor_1  city_neighbor_2  ...
         * for each city.
         * <p/>
         * Also modifies the cityPopulations map.
         *
         * @param file The file we are loading from
         * @return Map from city id (city_country,city_state,city_name) to city neighbors
         */
        public Map<String, Set<Integer>> loadNeighborFile(File file) {
            Map<String, Set<Integer>> result = new HashMap<String, Set<Integer>>();
            cityPopulations = new HashMap<String, Double>();
            try {
                Scanner scan = new Scanner(file);
                while (scan.hasNextLine()) {
                    String line = scan.nextLine();
                    String[] array = line.split("\t");
                    Set<Integer> set = new HashSet<Integer>();
                    String id = array[0];
                    double population = Double.parseDouble(array[1]);
                    for (int i = 2; i < array.length; i++) {
                        set.add(Integer.parseInt(array[i]));
                    }
                    result.put(id, set);
                    cityPopulations.put(id, population);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }



        /**
         * Given sets of familiar points for each location it is possible for a turker to be from, use known turker country
         * demographics, and within that, city populations, to generate turkers with reasonable familiar location sets.
         *
         * @param neighbors        Map from city_ids (country,state,city) to sets of neighboring universal concept ids
         * @param citiesPopulation Map from city_ids to their populations
         * @param numTurkers       Number of turkers to generate
         * @return A list of sets of concept ids, where each set represents the known concept ids of a simulated turker
         */
        public List<Set<Integer>> generateSimulatedTurkers(Map<String, Set<Integer>> neighbors, Map<String, Double> citiesPopulation, int numTurkers) {
            List<Set<Integer>> listOfTurkerFamiliarIds = new ArrayList<Set<Integer>>();

            //if population of a city is not available, set it to small.
            double small = 10;

            // COUNTRY populations
            Map<String, Double> countryPopulation = new HashMap<String, Double>();

            // get country populations by adding up city populations
            for (String cityName : citiesPopulation.keySet()) {
                String countryName = cityName.substring(0, cityName.indexOf(','));
                double pop = citiesPopulation.get(cityName);
                if (pop < small) {
                    pop = small;
                }
                if (countryPopulation.get(countryName) == null) {
                    countryPopulation.put(countryName, pop);
                } else {
                    countryPopulation.put(countryName, countryPopulation.get(countryName) + pop);
                }
            }

            // get world population by adding up relevant country populations
            double total = 0;
            for (String countryName : countryPopulation.keySet()) {
                total += countryPopulation.get(countryName);
            }

            // get country percent of world population
            Map<String, Double> countryPopPercent = new HashMap<String, Double>();
            for (String countryName : countryPopulation.keySet()) {
                countryPopPercent.put(countryName, (countryPopulation.get(countryName) / total));
            }

            // Turker demographic percentages
            // from http://www.appappeal.com/maps/amazon-mechanical-turk
            Map<String, Double> countryTurkPercent = new HashMap<String, Double>();
            countryTurkPercent.put("United States of America", 51.5);
            countryTurkPercent.put("United Kingdom", 1.9);
            countryTurkPercent.put("India", 33.0);
            countryTurkPercent.put("Brazil", 0.8);
            countryTurkPercent.put("Australia", 0.9);
            countryTurkPercent.put("Pakistan", 2.0);
            countryTurkPercent.put("Spain", 0.6);
            countryTurkPercent.put("Canada", 0.7);
            countryTurkPercent.put("France", 0.5);

            // make weighted random collection
            RandomCollection<String> randomCollection = new RandomCollection<String>();
            for (String cityName : citiesPopulation.keySet()) {
                double pop = citiesPopulation.get(cityName);
                if (pop < small) {
                    pop = small;
                }
                String countryName = cityName.substring(0, cityName.indexOf(','));
                // get population as percent of country population
                pop /= countryPopPercent.get(countryName);
                // multiply by turker demographic percentage
                pop *= countryTurkPercent.get(countryName);
                randomCollection.add(pop, cityName);
            }


            try {
                PrintWriter writer = new PrintWriter(new FileWriter("TurkGeneratedCities.txt"));
                // generate turkers
                for (int i = 0; i < numTurkers; i++) {
                    Set<Integer> familiar = new HashSet<Integer>();
                    double d = 3*Math.random();
                    writer.print("Turker Number " + i + "\t");
                    for(int j=0; j<d; j++){
                        String city = randomCollection.next();
                        writer.print(city +",\t");
                        familiar.addAll(neighbors.get(city));
                    }
                    writer.println();
                    // turker id, country,state,city, city population
                    listOfTurkerFamiliarIds.add(familiar);
                    if (TurkersWithKnownIDs) {
                        writer.println("\tAnd Known IDs Are:");
                        for (Integer id : familiar) {
                            writer.println("\t\t\t" + idToStringMap.get(id));
                        }
                    }
                }
                writer.close();
            }catch (Exception e) {
            }

            // return the list
            return listOfTurkerFamiliarIds;
        }



        // random collection that respects weights
        private class RandomCollection<E> {
            private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
            private final Random random;
            private double total = 0;

            public RandomCollection() {
                this(new Random());
            }

            public RandomCollection(Random random) {
                this.random = random;
            }

            public void add(double weight, E result) {
                if (weight <= 0) return;
                total += weight;
                map.put(total, result);
            }

            /**
             * Return an element at random, based on the weight provided
             * when an element was added to the collection.
             *
             * @return
             */
            public E next() {
                double value = random.nextDouble() * total;
                return map.ceilingEntry(value).getValue();
            }
        }



        // inner class to keep city information temporarily together
        private class City {
            private String name;
            private Geometry geo;
            private Integer pop;
            private String country;
            private String state;

            public City(String name, Geometry geo, Integer pop, String country, String state) {
                this.name = name;
                this.geo = geo;
                this.pop = pop;
                this.country = country;
                this.state = state;
            }

            public String toString() {
                return country + "," + state + "," + name;
            }
        }

        // count how many neighbors the cities have
        private int[] neighborsCount;

        private void resetNeighborsCount(int numPossible) {
            neighborsCount = new int[numPossible + 1];
        }

        private synchronized void increment(int index) {
            neighborsCount[index]++;
        }
    }


