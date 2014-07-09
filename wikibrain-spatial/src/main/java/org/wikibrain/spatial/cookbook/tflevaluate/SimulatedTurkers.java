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
import org.wikibrain.core.lang.LanguageSet;
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

    public SimulatedTurkers(MatrixGenerator mg, Env env) {
        this.mg = mg;
        this.env = env;
    }

    public static void main(String[] args) throws ConfigurationException {
        Env env = new EnvBuilder().envFromArgs(args);
        Configurator c = env.getConfigurator();
        Language language = Language.getByLangCode(c.get(LanguageSet.class).getLangCodes().get(0));
        MatrixGenerator mg = new MatrixGenerator(env);

        SimulatedTurkers st = new SimulatedTurkers(mg, env);

        // set this boolean to change the purpose of this program
        boolean loadData = true;


        if (loadData) {
//        try {
//            st.parseGazetteer();
//            Map<String, Set<Integer>> map = st.getNearConceptList(10, 2 );
//            st.createNeighborFile(map, "citiesToNeighbors3.txt");
//            for (int i = 0; i<st.neighborsCount.length; i++){
//                System.out.print(i+":"+st.neighborsCount[i]+"\t");
//            }
            Map<String, Set<Integer>> neighbors = st.loadNeighborFile(new File("citiesToNeighbors3.txt"));
            for (String s : neighbors.keySet()) {
                st.neighborsCount[neighbors.get(s).size()]++;
            }
            for (int i = 0; i < st.neighborsCount.length; i++) {
                System.out.println(i + "\t" + st.neighborsCount[i]);
            }

//        } catch (IOException e){
//
//        }
        } else {


//        try {
//            mg.getGeoDataFromCities(new File("/scratch/ne_10m_populated_places/ne_10m_populated_places.shp"));
//            Map<String, Set<Integer>> map = mg.getNearConceptList(10, 2 );
//            mg.createNeighborFile(map);
            Map<String,Set<Integer>> neighbors = st.loadNeighborFile(new File("citiesToNeighbors3.txt"));
//
            //set number of simulated turkers to 1000
            List<Set<Integer>> simulatedTurkers = st.generateSimulatedTurkers(neighbors, st.cityPopulations, 1000);

            SurveyQuestionGenerator generator = new SurveyQuestionGenerator();
            List<Integer> knownIds = new ArrayList<Integer>();

            int kk = 0;
            int ku = 0;
            int uu = 0;
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new FileWriter("SimulatedTurkers.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Set<SpatialConceptPair> questionPairs = new HashSet<SpatialConceptPair>();
            int[] duplicates = new int[50];

            for (int i = 0; i < simulatedTurkers.size(); i++) {
                int count = 0;
                knownIds.clear();
                knownIds.addAll(simulatedTurkers.get(i));
                List<SpatialConceptPair> questions = generator.getConceptPairsToAsk(knownIds, i);
                Set<SpatialConceptPair> previous = new HashSet<SpatialConceptPair>();
                for (SpatialConceptPair question : questions) {
                    SpatialConcept concept1 = question.getFirstConcept();
                    SpatialConcept concept2 = question.getSecondConcept();

                    // id, c1, c1_known, c1_title, c2, c2_known, c2_title, ans (default is 0.0)
                    String s = question.toString();
                    questionPairs.add(question);
                    if (previous.contains(question)) {
                        count++;
                    } else {
                        previous.add(question);
                    }

                    if (knownIds.contains(concept1.getUniversalID()) && knownIds.contains(concept2.getUniversalID())) {
                        kk++;
                    } else if ((!knownIds.contains(concept1.getUniversalID())) && (!knownIds.contains(concept2.getUniversalID()))) {
                        uu++;
                    } else {
                        ku++;
                    }
                    s = i + "\t" + s + "\t" + (knownIds.contains(concept1.getUniversalID())) + "\t" + (knownIds.contains(concept2.getUniversalID()));
                    pw.println(s);
                }
                if (count > 35) {
                    System.out.println(i + " " + count);
                }
                duplicates[count]++;
            }
            int count = 0;
            for (SpatialConceptPair pair : questionPairs) {
                if (pair.getkkTypeNumbOfTimesAsked() >= 10) {
                    System.out.println(pair.getFirstConcept().getTitle() + "\t" + pair.getSecondConcept().getTitle() + "\t" + pair.getkkTypeNumbOfTimesAsked() + "\t" + pair.getuuTypeNumbOfTimesAsked());
                    count++;
                }
            }
            System.out.println(count);
            try {
                st.analyzeQuestionPairs(questionPairs, c, language);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }

            System.out.println(questionPairs.size());
            System.out.println("known known " + kk);
            System.out.println("known unknown " + ku);
            System.out.println("unknown unknown " + uu);

            System.out.println(Arrays.toString(duplicates));
        }
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
     * An old method reading in a shape file to extract cities' populations and locations
     * It modifies fields cityGeometries and cityPopulations
     *
     * @param rawFile
     * @throws IOException
     */
    public void getGeoDataFromCities(File rawFile) throws IOException {
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
        int geometryField = 0, countryField = 17, stateField = 19, nameField = 5;
        // attribute 83 to 88 are populations from 1990 to 2015
        int minPopField = 83, maxPopField = 88;

        Set<String> countryNames = new HashSet<String>();
        String[] array = {"United States of America", "Canada", "Brazil", "Pakistan", "India", "France", "Spain", "United Kingdom", "Australia"};
        for (String s : array) {
            countryNames.add(s);
        }

        // loop over items
        while (inputFeatures.hasNext()) {
            SimpleFeature feature = inputFeatures.next();
            if (countryNames.contains(feature.getAttribute(countryField))) {

                // city id: country,state,city
                String id = feature.getAttribute(countryField) + "," + feature.getAttribute(stateField) + "," + feature.getAttribute(nameField);

                // city geometry
                Geometry g = (Geometry) feature.getAttribute(geometryField);
                cityGeometries.put(id, g);

                // city population
                double pop = 0;
                for (int j = minPopField; j <= maxPopField; j++) {
                    if ((Double) feature.getAttribute(j) != 0) {
                        pop = Math.max(pop, (Double) feature.getAttribute(j));
                    }
                }
                cityPopulations.put(id, pop);
            }
        }
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

        // generate turkers
        for (int i = 0; i < numTurkers; i++) {
            String city = randomCollection.next();
            // turker id, country,state,city, city population
            System.out.println(i + " " + city + " " + cityPopulations.get(city));
            listOfTurkerFamiliarIds.add(neighbors.get(city));
        }

        // return the list
        return listOfTurkerFamiliarIds;
    }

    /**
     * Analyzes the set of SpatialConceptPairs that were asked
     *
     * @param spatialConceptPairs
     * @param c
     * @throws ConfigurationException
     */
    public void analyzeQuestionPairs(Set<SpatialConceptPair> spatialConceptPairs, Configurator c, Language l) throws ConfigurationException {
        InstanceOfExtractor ioe = new InstanceOfExtractor(c,l);
        ioe.loadScaleIds();
        int[][][] bucketCounts = new int[InstanceOfExtractor.NUM_SCALES][InstanceOfExtractor.NUM_SCALES][6];
        for (SpatialConceptPair pair : spatialConceptPairs) {
            SpatialConcept concept1 = pair.getFirstConcept();
            SpatialConcept concept2 = pair.getSecondConcept();
            int id1 = concept1.getUniversalID();
            int id2 = concept2.getUniversalID();
            try {
                if (pair.getGraphDistance() <= 4) {
                    bucketCounts[ioe.getScale(id1)][ioe.getScale(id2)][(int) pair.getGraphDistance()]++;
                } else {
                    bucketCounts[ioe.getScale(id1)][ioe.getScale(id2)][5]++;
                }
            } catch (NullPointerException e) {
                System.out.println(id1 + " " + id2 + " cannot find scale");
            }
        }
//        for (int i = 0; i<InstanceOfExtractor.NUM_SCALES;i++){
//            for (int j = 0; j<InstanceOfExtractor.NUM_SCALES;j++){
//                for (int k = 0; k< 6; k++) {
//                    System.out.println(ioe.fileNames[i] + "\t" + ioe.fileNames[j] + "\t" + k+"\t"+bucketCounts[i][j][k]);
//                }
//            }
//        }
        for (int i = 0; i < InstanceOfExtractor.NUM_SCALES; i++) {
            System.out.print(ioe.fileNames[i] + "\t\t\t");
            for (int j = 0; j < InstanceOfExtractor.NUM_SCALES; j++) {
                int current = 0;
                for (int k = 0; k < 6; k++) {
                    current += bucketCounts[i][j][k];
                }
                System.out.print(current + "\t");
            }
            System.out.println();
        }
        for (int i = 0; i < 6; i++) {
            int current = 0;
            for (int j = 0; j < InstanceOfExtractor.NUM_SCALES; j++) {
                for (int k = 0; k < InstanceOfExtractor.NUM_SCALES; k++) {
                    current += bucketCounts[j][k][i];
                }
            }
            System.out.println(current + " pairs have distance " + i);
        }

    }

    /**
     * Find the concepts near each city with graph distance <= maxGraphDistance
     *
     * @param kNearestNeighbors Number of near neighbors to consider in each iteration of graph distance search
     * @param maxGraphDistance  Max depth to search to
     * @return Maps cities to the concept ids of the things "nearby"
     */
    public Map<String, Set<Integer>> getNearConceptList(final int kNearestNeighbors, final int maxGraphDistance) {
        // make final so that we can parallelize
        final Map<String, Geometry> citiesMap = cityGeometries;
        final Map<String, Set<Integer>> citiesToNeighbors = new HashMap<String, Set<Integer>>();

        // We will assume item ids (in 'geometries') are universal (because we think item_id in geometries is universal)
        final List<Geometry> citiesList = new ArrayList<Geometry>();
        citiesList.addAll(citiesMap.values());

        //distances between cities and significant geometries
        final float[][] citiesDistanceMatrix = mg.generateUnbalancedDistanceMatrix(citiesList);

        //distances between significant geometries
        final float[][] significantDistanceMatrix = mg.generateDistanceMatrix().getMatrix();
        System.out.println("Finished generating distance matrices");

        // reset array so that we can find out how many cities have each possible number of neighbors
        resetNeighborsCount(111);

        // for each city, calculate nearest neighbors
        ParallelForEach.loop(citiesMap.keySet(), new Procedure<String>() {
            @Override
            public void call(String city) throws Exception {

                try {
                    Set<Integer> set = mg.dm.getGraphDistance(mg.geometries, kNearestNeighbors, maxGraphDistance, significantDistanceMatrix, citiesDistanceMatrix[citiesList.indexOf(citiesMap.get(city))]);
                    citiesToNeighbors.put(city, set);
                    // here, we note that we found one more city that had set.size() neighbors
                    increment(set.size());
                } catch (DaoException e) {
                    e.printStackTrace();
                }
            }
        });
        return citiesToNeighbors;
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

    /**
     * This method parses the GeoNames Gazetteer Data to get city populations and locations.
     * (http://download.geonames.org/export/dump/)
     * It puts these in the fields cityPopulations and cityGeometries.
     *
     * @throws IOException
     */
    public void parseGazetteer() throws IOException {
        cityPopulations = new HashMap<String, Double>();
        cityGeometries = new HashMap<String, Geometry>();
        // all cities with population 1K+
        Scanner scanner = new Scanner(new File("cities1000.txt"));

        // country matching
        String[] array2 = {"United States of America", "Canada", "Brazil", "Pakistan", "India", "France", "Spain", "United Kingdom", "Australia"};
        String[] countryCodes = {"US", "CA", "BR", "PK", "IN", "FR", "ES", "GB", "AU"};
        // codes to country names
        Map<String, String> countryNames = new HashMap<String, String>();
        for (int i = 0; i < countryCodes.length; i++) {
            countryNames.put(countryCodes[i], array2[i]);
        }
        // set of codes
        Set<String> countries = new HashSet<String>();
        countries.addAll(Arrays.asList(countryCodes));

        // list of cities
        List<City> cityList = new ArrayList<City>();

        // read cities file
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] array = line.split("\t");
            String country = array[8];

            // if it's in a country we care about
            if (countries.contains(country)) {
                String name = array[1];
                String state = array[10];
                Double lat = Double.parseDouble(array[4]);
                Double longitude = Double.parseDouble(array[5]);
                Integer pop = Integer.parseInt(array[14]);

                // convert lat and long to a geometry
                Coordinate[] coords = new Coordinate[1];
                coords[0] = new Coordinate(longitude, lat);
                CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
                Point current = new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326));

                // store the information
                City city = new City(name, current, pop, country, state);
                cityList.add(city);
            }
        }

        // read in file that gives conversion from state code names to state actual names
        Map<String, String> stateCodesToNames = new HashMap<String, String>();
        Scanner scan = new Scanner(new File("admin1CodesASCII.txt"));
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] array = line.split("\t");
            stateCodesToNames.put(array[0], array[1]);
        }

        // convert state and country names from codes
        for (City city : cityList) {
            city.state = stateCodesToNames.get(city.country + "." + city.state);
            city.country = countryNames.get(city.country);
        }

        // store the information in cityGeometries and cityPopulations
        for (City city : cityList) {
            cityGeometries.put(city.toString(), city.geo);
            cityPopulations.put(city.toString(), (double) city.pop);
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
