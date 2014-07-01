package org.wikibrain.spatial.cookbook.tflevaluate;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.omg.CORBA.Environment;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.matrix.Matrix;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;
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

    private Map<Integer, Geometry> geometries;
    private DistanceMetrics dm;
    private MonolingualSRMetric sr;
    private Language simple = Language.SIMPLE;
    private static final Logger LOG = Logger.getLogger(InstanceOfExtractor.class.getName());
//    private List<Integer> list;
    private float[][] matrix;
    private List<Integer> pageHitList;
//    private Env env;

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
//        try {
//            Scanner scanner = new Scanner(new File("PageHitList.txt"));
//            while(scanner.hasNext()){
//                pageHitList.add(scanner.nextInt());
//            }
//        } catch(IOException e){
//            System.out.println("cannot find PageHitList.txt");
//        }
    }

    public static void main (String[] args){
        Env env = null;
        try{
            env = EnvBuilder.envFromArgs(args);
        }catch(ConfigurationException e){
            e.printStackTrace();
        }
        MatrixGenerator mg = new MatrixGenerator(env);
//
//        try {
//            Map<String, Set<Integer>> map = mg.getNearConceptList(mg.getGeoDataFromCities(new File("/scratch/cities2/cities.shp")), 10, 2 );
//            mg.createNeighborFile(map);
//        } catch(IOException e){
//            e.printStackTrace();
//        }

        MatrixWithHeader matrix = mg.generateSRMatrix();
        System.out.println("Finished generating matrix");
        mg.createMatrixFile("srmatrix",matrix);
        System.out.println("Finished writing matrix to file");
        MatrixWithHeader matrix2 = mg.loadMatrixFile("srmatrix");

        System.out.println("Finished loading matrix");

        System.out.println(matrix.idToIndex.get(30));

//        List<Integer> check = Arrays.asList(18426, 65,36091,34860,16554,38733,79842,496360, 85, 8678);
//        for (Integer i: check){
//            for (int j : check){
//                try {
//                    System.out.println("distance between " + i + " and " + j + " = " + matrix2.matrix[mg.pageHitList.indexOf(i)][mg.pageHitList.indexOf(j)]);
//                } catch(Exception e){
//
//                }
//            }
//        }

        for (int i=0; i<matrix.matrix.length; i++) {
            if (!Arrays.equals(matrix.matrix[i], matrix2.matrix[i])){
                System.out.println("Unequal row "+i);
            }
        }

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
//                        e2.printStackTrace();
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

        //getTopologicalDistance(Geometry a, Integer itemIdA, Geometry b, Integer itemIdB, int k, String layerName, String refSysName)

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

    private class MatrixWithHeader{
        private float[][] matrix;
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


    }

    public Map<String,Geometry> getGeoDataFromCities(File rawFile) throws IOException{
        Map<String, Geometry> result = new HashMap<String, Geometry>();

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

        // loop over items
        while (inputFeatures.hasNext()) {
            SimpleFeature feature = inputFeatures.next();
            // country,state,city
            String id = feature.getAttribute(6)+","+feature.getAttribute(4)+","+feature.getAttribute(2);
            System.out.println(id);
            Geometry g = (Geometry)feature.getAttribute(0);
            result.put(id,g);
        }

        return result;
    }

    public Map<String, Set<Integer>> getNearConceptList(final Map<String, Geometry> citiesMap, final int k, final int maxTopoDistance){
        final Map<String, Set<Integer>> result = new HashMap<String, Set<Integer>>();

        // We will assume item ids (in 'geometries') are universal (because we think item_id in geometries are universal)

        final List<Geometry> citiesList = new ArrayList<Geometry>();
        citiesList.addAll(citiesMap.values());
        final float[][] citiesDistanceMatrix = generateUnbalancedDistanceMatrix(citiesList);
        final float[][] significantDistanceMatrix = generateDistanceMatrix().matrix;
        System.out.println("Finished generating distance matrices");


        ParallelForEach.loop(citiesMap.keySet(), new Procedure<String>() {
            @Override
            public void call(String city) throws Exception {

                try {
                    Set<Integer> set = dm.getGraphDistance( geometries, k, maxTopoDistance, significantDistanceMatrix,citiesDistanceMatrix[citiesList.indexOf(citiesMap.get(city))]);
                    result.put(city,set);
                    System.out.println(city+": "+set.size());
//                    for (Integer i : set){
//                        System.out.print(i + "\t");
//                    }
//                    System.out.println();

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
//                        e2.printStackTrace();
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
            bw = new BufferedWriter(new FileWriter("citiesToNeighbors.txt"));

            for (String string: map.keySet()){
                bw.write(string+"\t");
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


}
