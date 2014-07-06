package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
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
import org.wikibrain.spatial.maxima.SpatialConcept;
import org.wikibrain.spatial.maxima.SpatialConceptPair;
import org.wikibrain.spatial.maxima.SurveyQuestionGenerator;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Created by maixa001 on 7/3/14.
 */
public class SimulatedTurkers {

    private Map<String, Double> cityPopulations;
    private Map<String, Geometry> cityGeometries;
    private MatrixGenerator mg;
    private Env env;

    public SimulatedTurkers( MatrixGenerator mg, Env env){
        this.mg = mg;
        this.env = env;
    }

    public static void main(String[] args) throws ConfigurationException{
        Env env = new EnvBuilder().envFromArgs(args);
        MatrixGenerator mg = new MatrixGenerator(env);

        SimulatedTurkers st = new SimulatedTurkers( mg, env);

//        try {
//            mg.getGeoDataFromCities(new File("/scratch/ne_10m_populated_places/ne_10m_populated_places.shp"));
//            Map<String, Set<Integer>> map = mg.getNearConceptList(10, 2 );
//            mg.createNeighborFile(map);
        Map<String,Set<Integer>> neighbors = st.loadNeighborFile(new File("citiesToNeighbors2.txt"));

        //set number of simulated turkers to 1000
        List<Set<Integer>> simulatedTurkers = st.generateSimulatedTurkers(neighbors, st.cityPopulations, 1000);

        SurveyQuestionGenerator generator = new SurveyQuestionGenerator();
        List<Integer> knownIds = new ArrayList<Integer>();

        int kk = 0;
        int ku = 0;
        int uu = 0;
        PrintWriter pw = null;
        try{
            pw = new PrintWriter(new FileWriter("SimulatedTurkers.txt"));
        }catch(IOException e){
            e.printStackTrace();
        }

        Set<SpatialConceptPair> questionPairs = new HashSet<SpatialConceptPair>();
        int[] duplicates = new int[50];

        for (int i=0; i<simulatedTurkers.size(); i++){
            int count = 0;
            knownIds.clear();
            knownIds.addAll(simulatedTurkers.get(i));
            List<SpatialConceptPair> questions = generator.getConceptPairsToAsk(knownIds,i);
            Set<SpatialConceptPair> previous = new HashSet<SpatialConceptPair>();
            for (SpatialConceptPair question: questions){
                SpatialConcept concept1 = question.getFirstConcept();
                SpatialConcept concept2 = question.getSecondConcept();

                // id, c1, c1_known, c1_title, c2, c2_known, c2_title, ans (default is 0.0)
                String s = question.toString();
                questionPairs.add(question);
                if (previous.contains(question)){
                    count++;
                } else {
                    previous.add(question);
                }

                if (knownIds.contains(concept1.getUniversalID())&&knownIds.contains(concept2.getUniversalID())){
                    kk++;
                } else if ((!knownIds.contains(concept1.getUniversalID()))&&(!knownIds.contains(concept2.getUniversalID()))) {
                    uu++;
                } else {
                    ku++;
                }
                s = i+"\t"+s+"\t"+(knownIds.contains(concept1.getUniversalID()))+"\t"+(knownIds.contains(concept2.getUniversalID()));
                pw.println(s);
            }
            if (count>35){
                System.out.println(i+" "+count);
            }
            duplicates[count]++;
        }
        int count = 0;
        for (SpatialConceptPair pair: questionPairs){
            if (pair.getkkTypeNumbOfTimesAsked()>=10){
                System.out.println(pair.getFirstConcept().getTitle()+"\t"+pair.getSecondConcept().getTitle()+"\t"+pair.getkkTypeNumbOfTimesAsked()+"\t"+pair.getuuTypeNumbOfTimesAsked());
                count++;
            }
        }
        System.out.println(count);
        try {
            st.analyzeQuestionPairs(questionPairs, env.getConfigurator());
        } catch (ConfigurationException e){
            e.printStackTrace();
        }

        System.out.println(questionPairs.size());
        System.out.println("known known "+kk);
        System.out.println("known unknown "+ku);
        System.out.println("unknown unknown "+uu);

        System.out.println(Arrays.toString(duplicates));
    }

    public void createNeighborFile(Map<String, Set<Integer>> map){

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("citiesToNeighbors2.txt"));

            for (String string: map.keySet()){
                bw.write(string+"\t"+cityPopulations.get(string)+"\t");
                for (Integer i : map.get(string)){
                    bw.write(i+"\t");
                }
                bw.write("\n");
            }
            bw.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }


    public Map<String,Set<Integer>> loadNeighborFile(File file){
        Map<String,Set<Integer>> result = new HashMap<String, Set<Integer>>();
        cityPopulations = new HashMap<String, Double>();
        try {
            Scanner scan = new Scanner(file);
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                String[] array = line.split("\t");
                Set<Integer> set = new HashSet<Integer>();
                String id = array[0];
                double population = Double.parseDouble(array[1]);
                for (int i=2; i<array.length; i++){
                    set.add(Integer.parseInt(array[i]));
                }
                result.put(id,set);
                cityPopulations.put(id,population);
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return result;
    }
    public void getGeoDataFromCities(File rawFile) throws IOException{
        cityGeometries = new HashMap<String, Geometry>();
        cityPopulations = new HashMap<String, Double>();

        // get SimpleFeatureIterator
        Map<String, URL> map = new HashMap<String, URL>();
        map.put("url", rawFile.toURI().toURL());
        DataStore inputDataStore = DataStoreFinder.getDataStore(map);
        SimpleFeatureSource inputFeatureSource = inputDataStore.getFeatureSource(inputDataStore.getTypeNames()[0]);
        SimpleFeatureCollection collection = inputFeatureSource.getFeatures();
        SimpleFeatureIterator inputFeatures = collection.features();

        // shape file fields:
        // geometry, city, country/state code, state, country code, country,
        // type of city, population, population category rank, population category, "port_id", "label_flag"
        Set<String> countryNames = new HashSet<String>();
        String[] array = {"United States of America","Canada","Brazil","Pakistan","India","France","Spain","United Kingdom","Australia"};
        for (String s: array){
            countryNames.add(s);
        }

        // loop over items
        while (inputFeatures.hasNext()) {
            SimpleFeature feature = inputFeatures.next();
            if (countryNames.contains(feature.getAttribute(17))) {

                // country,state,city
                String id = feature.getAttribute(17) + "," + feature.getAttribute(19) + "," + feature.getAttribute(5);
                System.out.println(id);
                Geometry g = (Geometry) feature.getAttribute(0);
                cityGeometries.put(id, g);

                double pop = 0;
                // attribute 83 to 88 are populations from 1990 to 2015
                for (int j=83; j<=88; j++){
                    if ((Double)feature.getAttribute(j)!=0){
                        pop = Math.max(pop,(Double)feature.getAttribute(j));
                    }
                }
                cityPopulations.put(id,pop);
            }
        }
    }



    public List<Set<Integer>> generateSimulatedTurkers(Map<String,Set<Integer>> neighbors, Map<String,Double> citiesPopulation, int numTurkers){
        List<Set<Integer>> result = new ArrayList<Set<Integer>>();

        //if population of a city is not available, set it to small.
        double small = 10;

        Map<String,Double> countryPopulation = new HashMap<String, Double>();

        // get country populations
        for (String cityName:citiesPopulation.keySet()){
            String countryName = cityName.substring(0, cityName.indexOf(','));
            double pop = citiesPopulation.get(cityName);
            if (pop == 0){
                pop = small;
            }
            if (countryPopulation.get(countryName)==null){
                countryPopulation.put(countryName,pop);
            }else{
                countryPopulation.put(countryName,countryPopulation.get(countryName)+pop);
            }
        }

        // get world population
        double total = 0;
        for (String countryName:countryPopulation.keySet()){
            total += countryPopulation.get(countryName);
//            System.out.println(countryName+" "+countryPopulation.get(countryName));
        }
//        System.out.println("total = "+total);

        // get country percent population
        Map<String,Double> countryPopPercent = new HashMap<String, Double>();
        for (String countryName:countryPopulation.keySet()){
            countryPopPercent.put(countryName,(countryPopulation.get(countryName)/total));
//            System.out.println(countryName+" "+(countryPopulation.get(countryName)/total));
        }

        // from http://www.appappeal.com/maps/amazon-mechanical-turk
        Map<String,Double> countryTurkPercent = new HashMap<String, Double>();
        countryTurkPercent.put("United States of America",51.5);
        countryTurkPercent.put("United Kingdom",1.9);
        countryTurkPercent.put("India",33.0);
        countryTurkPercent.put("Brazil",0.8);
        countryTurkPercent.put("Australia",0.9);
        countryTurkPercent.put("Pakistan",2.0);
        countryTurkPercent.put("Spain",0.6);
        countryTurkPercent.put("Canada",0.7);
        countryTurkPercent.put("France",0.5);

        // make weighted random collection
        RandomCollection<String> randomCollection = new RandomCollection<String>();
        for (String cityName : citiesPopulation.keySet()){
            double pop = citiesPopulation.get(cityName);
            if (pop == 0){
                pop = small;
            }
            String countryName = cityName.substring(0, cityName.indexOf(','));
            pop /= countryPopPercent.get(countryName);
            pop *= countryTurkPercent.get(countryName);
            randomCollection.add(pop, cityName);
        }

        // generate turkers
        for (int i = 0; i< numTurkers; i++){
            String city = randomCollection.next();
            System.out.println(i+" "+city+" "+cityPopulations.get(city));
            result.add(neighbors.get(city));
        }
        return result;
    }

    public void analyzeQuestionPairs(Set<SpatialConceptPair> spatialConceptPairs, Configurator c) throws ConfigurationException {
        InstanceOfExtractor ioe = new InstanceOfExtractor(c);
        ioe.loadScaleIds();
        int[][][] bucketCounts = new int[InstanceOfExtractor.NUM_SCALES][InstanceOfExtractor.NUM_SCALES][6];
        for (SpatialConceptPair pair: spatialConceptPairs){
            SpatialConcept concept1 = pair.getFirstConcept();
            SpatialConcept concept2 = pair.getSecondConcept();
            int id1 = concept1.getUniversalID();
            int id2 = concept2.getUniversalID();
            try {
                if (pair.getGraphDistance()<=4) {
                    bucketCounts[ioe.getScale(id1)][ioe.getScale(id2)][(int) pair.getGraphDistance()]++;
                } else {
                    bucketCounts[ioe.getScale(id1)][ioe.getScale(id2)][5]++;
                }
            } catch (NullPointerException e){
                System.out.println(id1+" "+id2+" cannot find scale");
            }
        }
//        for (int i = 0; i<InstanceOfExtractor.NUM_SCALES;i++){
//            for (int j = 0; j<InstanceOfExtractor.NUM_SCALES;j++){
//                for (int k = 0; k< 6; k++) {
//                    System.out.println(ioe.fileNames[i] + "\t" + ioe.fileNames[j] + "\t" + k+"\t"+bucketCounts[i][j][k]);
//                }
//            }
//        }
        for (int i = 0; i<InstanceOfExtractor.NUM_SCALES;i++){
            System.out.print(ioe.fileNames[i]+"\t\t\t");
            for (int j = 0; j<InstanceOfExtractor.NUM_SCALES;j++) {
                int current = 0;
                for (int k = 0; k< 6; k++) {
                    current += bucketCounts[i][j][k];
                }
                System.out.print(current + "\t");
            }
            System.out.println();
        }
        for (int i = 0; i<6;i++){
            int current = 0;
            for (int j = 0; j<InstanceOfExtractor.NUM_SCALES;j++){
                for (int k = 0; k< InstanceOfExtractor.NUM_SCALES; k++) {
                    current += bucketCounts[j][k][i];
                }
            }
            System.out.println(current+" pairs have distance "+i);
        }

    }

    public Map<String, Set<Integer>> getNearConceptList(final int k, final int maxTopoDistance){
        final Map<String, Geometry> citiesMap = cityGeometries;
        final Map<String, Set<Integer>> result = new HashMap<String, Set<Integer>>();

        // We will assume item ids (in 'geometries') are universal (because we think item_id in geometries are universal)

        final List<Geometry> citiesList = new ArrayList<Geometry>();
        citiesList.addAll(citiesMap.values());

        //distances between cities and significant geometries
        final float[][] citiesDistanceMatrix = mg.generateUnbalancedDistanceMatrix(citiesList);

        //distances between significant geometries
        final float[][] significantDistanceMatrix = mg.generateDistanceMatrix().getMatrix();
        System.out.println("Finished generating distance matrices");

        ParallelForEach.loop(citiesMap.keySet(), new Procedure<String>() {
            @Override
            public void call(String city) throws Exception {

                try {
                    Set<Integer> set = mg.dm.getGraphDistance(mg.geometries, k, maxTopoDistance, significantDistanceMatrix, citiesDistanceMatrix[citiesList.indexOf(citiesMap.get(city))]);
                    result.put(city, set);
                    System.out.println(city + ": " + set.size());
                } catch (DaoException e) {
                    e.printStackTrace();
                }
                System.out.println("Processed city " + city);
            }
        });
        return result;
    }


    // random with weights
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

        public E next() {
            double value = random.nextDouble() * total;
            return map.ceilingEntry(value).getValue();
        }
    }
}
