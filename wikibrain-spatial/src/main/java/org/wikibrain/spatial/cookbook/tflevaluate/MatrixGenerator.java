package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.opengis.feature.simple.SimpleFeature;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;
import org.wikibrain.spatial.maxima.SpatialConcept;
import org.wikibrain.spatial.maxima.SpatialConceptPair;
import org.wikibrain.spatial.maxima.SurveyQuestionGenerator;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by maixa001 on 6/24/14.
 */
public class MatrixGenerator {

    public Map<Integer, Geometry> geometries;
    public DistanceMetrics dm;
    private MonolingualSRMetric sr;
    private Language simple = Language.SIMPLE;
    private static final Logger LOG = Logger.getLogger(InstanceOfExtractor.class.getName());
//    private List<Integer> list;
    private float[][] matrix;
    private List<Integer> pageHitList;
//    private Env env;
    private Map<String,Geometry> cityGeometries;
    private Map<String,Double> cityPopulations;

    public MatrixGenerator(Env env){
        SpatialDataDao sdDao = null;
        SpatialNeighborDao snDao = null;
        Configurator c = env.getConfigurator();
        try{
            sdDao= c.get(SpatialDataDao.class);
            snDao = c.get(SpatialNeighborDao.class);

        }catch(ConfigurationException e){
            e.printStackTrace();
        }
        // eventually, do something to geometries to make it have only significant entries
        try {
            this.geometries = sdDao.getAllGeometriesInLayer("significant", "earth");
        }catch(DaoException e){
            e.printStackTrace();
        }
        dm = new DistanceMetrics(env, c, snDao);
        try {
            sr = c.get(
                    MonolingualSRMetric.class, "ensemble",
                    "language", simple.getLangCode());
        } catch(ConfigurationException e){
            e.printStackTrace();
        }
        pageHitList = new ArrayList<Integer>();
        pageHitList.addAll(geometries.keySet());
    }

    public static void main (String[] args){
        Env env = null;
        try{
            env = EnvBuilder.envFromArgs(args);
        }catch(ConfigurationException e){
            e.printStackTrace();
        }
        MatrixGenerator mg = new MatrixGenerator(env);

//        try {
//            mg.getGeoDataFromCities(new File("/scratch/ne_10m_populated_places/ne_10m_populated_places.shp"));
//            Map<String, Set<Integer>> map = mg.getNearConceptList(10, 2 );
//            mg.createNeighborFile(map);
        Map<String,Set<Integer>> neighbors = mg.loadNeighborFile(new File("citiesToNeighbors2.txt"));

        //set number of simulated turkers to 1000
        List<Set<Integer>> simulatedTurkers = mg.generateSimulatedTurkers(neighbors,mg.cityPopulations,1000);

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

        for (int i=0; i<simulatedTurkers.size(); i++){
            knownIds.clear();
            knownIds.addAll(simulatedTurkers.get(i));
            List<SpatialConceptPair> questions = generator.getConceptPairsToAsk(knownIds,i);
            for (SpatialConceptPair question: questions){
                SpatialConcept concept1 = question.getFirstConcept();
                SpatialConcept concept2 = question.getSecondConcept();

                // id, c1, c1_known, c1_title, c2, c2_known, c2_title, ans (default is 0.0)
                String s = question.toString();
                questionPairs.add(question);


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
            mg.analyzeQuestionPairs(questionPairs, env.getConfigurator());
        } catch (ConfigurationException e){
            e.printStackTrace();
        }

        System.out.println(questionPairs.size());
        System.out.println("known known "+kk);
        System.out.println("known unknown "+ku);
        System.out.println("unknown unknown "+uu);

//        MatrixWithHeader matrix = mg.generateSRMatrix();
//        System.out.println("Finished generating matrix");
//        mg.createMatrixFile("srmatrix",matrix);
//        System.out.println("Finished writing matrix to file");
//        MatrixWithHeader matrix2 = mg.loadMatrixFile("srmatrix");
//
//        System.out.println("Finished loading matrix");
//
//        System.out.println(matrix.idToIndex.get(30));

//        List<Integer> check = Arrays.asList(18426, 65,36091,34860,16554,38733,79842,496360, 85, 8678);
//        for (Integer i: check){If M and N are any two topological spaces, then the Euler characteristic of their disjoint union is the sum of their Euler characteristics, since homology is additive under disjoint union:
//            for (int j : check){
//                try {
//                    System.out.println("distance between " + i + " and " + j + " = " + matrix2.matrix[mg.pageHitList.indexOf(i)][mg.pageHitList.indexOf(j)]);
//                } catch(Exception e){
//
//                }
//            }
//        }

//        for (int i=0; i<matrix.matrix.length; i++) {
//            if (!Arrays.equals(matrix.matrix[i], matrix2.matrix[i])){
//                System.out.println("Unequal row "+i);
//            }
//        }
    }

    public MatrixWithHeader generateDistanceMatrix(){
        int size = pageHitList.size();
        float[][] matrix = new float[size][size];
        GeodeticCalculator calc = new GeodeticCalculator();
        List<Integer> list = pageHitList;
        for (int i = 0; i<size;i++){
            if (i%100==0){
                LOG.log(Level.INFO, "Processed "+i+" geometries");
            }
            try {
                Point point1 = (Point) geometries.get(list.get(i));
                calc.setStartingGeographicPoint(point1.getX(), point1.getY());
                for (int j = i+1; j < size; j++) {
                    float distance = 0;
                    try {
                        Point point2 = (Point) geometries.get(list.get(j));
                        calc.setDestinationGeographicPoint(point2.getX(), point2.getY());

                        try {
                            distance = (float) (calc.getOrthodromicDistance() / 1000);
                        } catch (ArithmeticException e) {
                            try {
                                distance = (float) (DefaultEllipsoid.WGS84.orthodromicDistance(point1.getX(), point1.getY(), point2.getX(), point2.getY()) / 1000);
                            } catch (ArithmeticException e2) {
                                distance = 20000;
                            }
                        }
                    }catch(NullPointerException e){
                        System.out.println("Null pointer exception for "+list.get(j));
                    }
                    matrix[i][j] = distance;
                    matrix[j][i] = distance;
                }
            } catch (NullPointerException e){
                System.out.println("no geometry for point "+list.get(i));
            }

        }
        return new MatrixWithHeader(matrix,list);
    }

    public MatrixWithHeader generateGraphMatrix(final int k, final int maxTopoDistance){
        int size = pageHitList.size();
        final float[][] matrix = new float[size][size];

        // We will assume item ids are universal (because we think item_id in geometries are universal)

        MatrixWithHeader distanceMatrixWithHeader = generateDistanceMatrix();
        System.out.println("Finished generating distance matrix");
        final float[][] distanceMatrix = distanceMatrixWithHeader.matrix;

        ParallelForEach.loop(pageHitList, new Procedure<Integer>() {
            @Override
            public void call(Integer id1) throws Exception {

                int i = pageHitList.indexOf(id1);

                try {
                    Map<Integer, Integer> topoDistance = dm.getGraphDistance(id1, geometries, k, maxTopoDistance, distanceMatrix) ;
                    for (int j = 0; j < distanceMatrix.length; j++) {
                        if (topoDistance.get(pageHitList.get(j))!=null) {
                            matrix[i][j] = topoDistance.get(pageHitList.get(j));
                        } else {
                            matrix[i][j] = Float.POSITIVE_INFINITY;
                        }
                    }
                } catch (DaoException e){
                    e.printStackTrace();
                }
                System.out.println("Processed row "+i);
            }
        });
        return new MatrixWithHeader(matrix,pageHitList);
    }

    public MatrixWithHeader generateSRMatrix(){
        int size = pageHitList.size();
        matrix = new float[size][size];

            ParallelForEach.loop(pageHitList, new Procedure<Integer>() {
                @Override
                public void call(Integer id1) throws Exception {

                    int i = pageHitList.indexOf(id1);
                    for (int j=0; j<pageHitList.size(); j++) {

                        float distance = 0;
                        try {
                            SRResult similarity = sr.similarity(id1, pageHitList.get(j), false);
                            distance = (float) similarity.getScore();
                        } catch (DaoException e) {
                            e.printStackTrace();
                        }

                        matrix[i][j] = distance;
                    }
                    System.out.println("Processed row "+i);
                }
            });

        return new MatrixWithHeader(matrix,pageHitList);
    }

    /**
     * Writes out a MatrixWithHeader to a binary file
     * The first four bytes are the dimension
     * The first 4*dimension bytes are the ids corresponding to the rows/columns
     * Next 4*dimension*dimension bytes are the float values in the matrix
     *
     * @param fileName
     * @param matrixWithHeader
     */
    public void createMatrixFile(String fileName, MatrixWithHeader matrixWithHeader){
        try {
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(fileName));

                // write dimension
                output.write(intArrayToByteArray(new Integer[] {matrixWithHeader.dimension}));

                // write ids
                output.write(intArrayToByteArray(matrixWithHeader.idsInOrder.toArray()));

                // write values
                for (int i=0; i<matrixWithHeader.matrix.length; i++){
                    output.write(floatArrayToByteArray(matrixWithHeader.matrix[i]));
                }
            }
            finally {
                output.close();
            }
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public MatrixWithHeader loadMatrixFile(String fileName ){

        File file = new File(fileName);

        // what we read from the file
        byte[] result;
        float[][] matrix = null;

        // list of integers
        List<Integer> ints = null;

        // mostly copied from http://www.javapractices.com/topic/TopicAction.do?Id=245
        try {
            InputStream input = null;
            try {
                long totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                int i = 0;

                byte[] dimensionArray = new byte[4];

                // read dimensions
                int bytesRead = input.read(dimensionArray, 0, 4);
                if (bytesRead != 4){
                    System.out.println("Error reading in file");
                    return null;
                }
                int dimension = byteArrayToIntArray(dimensionArray)[0];

                int byteDimension = dimension*4;
                long expectedFileSize = (dimension+1)*(long)byteDimension;
                result = new byte[byteDimension];
                matrix = new float[dimension][dimension];

                if (file.length()!=expectedFileSize+4){
                    System.out.println("Expected file size "+expectedFileSize+" but got "+(file.length()-4));
//            return null;
                }

                // try to read all the bytes
                while(totalBytesRead < expectedFileSize){
                    long bytesRemaining = expectedFileSize - totalBytesRead;
                    if (bytesRemaining<dimension){
                        System.out.println("Unexpected array size: "+bytesRemaining+" bytesRemaining, although dimension is "+dimension);
                    }
                    //input.read() returns -1, 0, or more :
                    bytesRead = input.read(result, 0, (int)Math.min(bytesRemaining,byteDimension));

                    if (bytesRead != byteDimension){
                        System.out.println("Read "+totalBytesRead+"/"+expectedFileSize);
                        System.out.println("Read wrong number of bytes--array will be wrong: read "+bytesRead+", wanted "+byteDimension);
                        return null;
                    }

                    // if anything got read this round
                    if (bytesRead > 0){
                        if (i==0){
                            ints = Arrays.asList(ArrayUtils.toObject(byteArrayToIntArray(result)));
                        }else {
                            matrix[i - 1] = byteArrayToFloatArray(result);
                        }
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                    i++;
                }
                System.out.println("Num bytes read: " + totalBytesRead);

            }
            finally {
                System.out.println("Closing input stream.");
                input.close();
            }

//            // break up the byte array into:
//            // ids in order
//            ints = Arrays.asList(ArrayUtils.toObject(byteArrayToIntArray(Arrays.copyOfRange(result, 0, dimension*4))));
//
//            // rows in float[][] matrix
//            for (int i=1; i<=dimension; i++){
//                matrix[i-1] = byteArrayToFloatArray(Arrays.copyOfRange(result, i*dimension*4, i*dimension*4+dimension*4));
//            }
        }
        catch (FileNotFoundException ex) {
            System.out.println("File not found.");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return new MatrixWithHeader(matrix,ints);
    }

    public byte[] floatArrayToByteArray(float[] values){
        ByteBuffer buffer = ByteBuffer.allocate(values.length*4).order(ByteOrder.BIG_ENDIAN);
        for (int i=0; i<values.length; i++){
            buffer.putFloat(i*4,values[i]);
        }
        return buffer.array();
    }

    public byte[] intArrayToByteArray(Object[] values){
        ByteBuffer buffer = ByteBuffer.allocate(values.length*4).order(ByteOrder.BIG_ENDIAN);
        for (int i=0; i<values.length; i++){
            buffer.putInt(i * 4, (Integer) values[i]);
        }
        return buffer.array();
    }

    public float[] byteArrayToFloatArray(byte[]values){
        FloatBuffer buffer = ByteBuffer.wrap(values).order(ByteOrder.BIG_ENDIAN).asFloatBuffer();
        float[] array = new float[values.length/4];
        buffer.get(array);
        return array;
    }

    public int[] byteArrayToIntArray(byte[]values){
        IntBuffer buffer = ByteBuffer.wrap(values).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        int[] array = new int[values.length/4];
        buffer.get(array);
        return array;
    }

    public class MatrixWithHeader{
        public float[][] matrix;
        public Map<Integer, Integer> idToIndex;
        private List<Integer> idsInOrder;
        private int dimension;

        public MatrixWithHeader(float[][] matrix, List<Integer> idsInOrder){
            this.matrix = matrix;
            this.idsInOrder = idsInOrder;
            // generate map
            idToIndex = new HashMap<Integer, Integer>();
            for (int i=0; i<idsInOrder.size(); i++){
                idToIndex.put(idsInOrder.get(i),i);
            }
            dimension = matrix.length;
        }

        public float[][] getMatrix(){
            return matrix;
        }
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

    public Map<String, Set<Integer>> getNearConceptList(final int k, final int maxTopoDistance){
        final Map<String, Geometry> citiesMap = cityGeometries;
        final Map<String, Set<Integer>> result = new HashMap<String, Set<Integer>>();

        // We will assume item ids (in 'geometries') are universal (because we think item_id in geometries are universal)

        final List<Geometry> citiesList = new ArrayList<Geometry>();
        citiesList.addAll(citiesMap.values());

        //distances between cities and significant geometries
        final float[][] citiesDistanceMatrix = generateUnbalancedDistanceMatrix(citiesList);

        //distances between significant geometries
        final float[][] significantDistanceMatrix = generateDistanceMatrix().matrix;
        System.out.println("Finished generating distance matrices");

        ParallelForEach.loop(citiesMap.keySet(), new Procedure<String>() {
            @Override
            public void call(String city) throws Exception {

                try {
                    Set<Integer> set = dm.getGraphDistance( geometries, k, maxTopoDistance, significantDistanceMatrix,citiesDistanceMatrix[citiesList.indexOf(citiesMap.get(city))]);
                    result.put(city,set);
                    System.out.println(city+": "+set.size());
                } catch (DaoException e){
                    e.printStackTrace();
                }
                System.out.println("Processed city "+city);
            }
        });
        return result;
    }

    public float[][] generateUnbalancedDistanceMatrix(List<Geometry> cities){
        int size = pageHitList.size();
        float[][] matrix = new float[cities.size()][size];
        GeodeticCalculator calc = new GeodeticCalculator();

        for (int i = 0; i<cities.size();i++){

            Point point1= (Point) cities.get(i);
            try {
                calc.setStartingGeographicPoint(point1.getX(), point1.getY());
                for (int j = 0; j < size; j++) {
                    float distance = 0;
                    try {
                        Point point2 = (Point) geometries.get(pageHitList.get(j));
                        calc.setDestinationGeographicPoint(point2.getX(), point2.getY());

                        try {
                            distance = (float) (calc.getOrthodromicDistance() / 1000);
                        } catch (ArithmeticException e) {
                            try {
                                distance = (float) (DefaultEllipsoid.WGS84.orthodromicDistance(point1.getX(), point1.getY(), point2.getX(), point2.getY()) / 1000);
                            } catch (ArithmeticException e2) {
                                //set default distance to be 20000 if 2 points are approximately opposite
                                distance = 20000;
                            }
                        }
                    }catch(NullPointerException e){
                        System.out.println("Null pointer exception for "+pageHitList.get(j));
                    }
                    matrix[i][j] = distance;
                }
            } catch (NullPointerException e){
                System.out.println("no geometry for point "+pageHitList.get(i));
            }
        }
        return matrix;
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
            System.out.println(countryName+" "+countryPopulation.get(countryName));
        }
        System.out.println("total = "+total);

        // get country percent population
        Map<String,Double> countryPopPercent = new HashMap<String, Double>();
        for (String countryName:countryPopulation.keySet()){
            countryPopPercent.put(countryName,(countryPopulation.get(countryName)/total));
            System.out.println(countryName+" "+(countryPopulation.get(countryName)/total));
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
        for (int i = 0; i<InstanceOfExtractor.NUM_SCALES;i++){
            for (int j = 0; j<InstanceOfExtractor.NUM_SCALES;j++){
                for (int k = 0; k< 6; k++) {
                    System.out.println(ioe.fileNames[i] + "\t" + ioe.fileNames[j] + "\t" + k+"\t"+bucketCounts[i][j][k]);
                }
            }
        }
        for (int i = 0; i<InstanceOfExtractor.NUM_SCALES;i++){
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
