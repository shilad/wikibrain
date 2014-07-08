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
    private float[][] matrix;
    public List<Integer> pageHitList;

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
        } catch (NullPointerException e){
            System.out.println("no such layer");
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
//        for (Integer i: check){
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
                            System.out.println(id1 +" and "+pageHitList.get(j)+": "+distance);
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

    public float[][] generateUnbalancedDistanceMatrix(final List<Geometry> cities){
        final int size = pageHitList.size();
        final float[][] matrix = new float[cities.size()][size];

        ParallelForEach.loop(cities, new Procedure<Geometry>() {
            @Override
            public void call(Geometry geo) throws Exception {

                int i = cities.indexOf(geo);

                Point point1= (Point) geo;
                try {
                    GeodeticCalculator calc = new GeodeticCalculator();
                    calc.setStartingGeographicPoint(point1.getX(), point1.getY());

                    // change ending geographic points
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
                                    //set default distance to be 20000 if 2 points are approximately opposite on the globe
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
        });

        return matrix;
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
    public static void createMatrixFile(String fileName, MatrixWithHeader matrixWithHeader){
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

    public static MatrixWithHeader loadMatrixFile(String fileName ){

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

    public static byte[] floatArrayToByteArray(float[] values){
        ByteBuffer buffer = ByteBuffer.allocate(values.length*4).order(ByteOrder.BIG_ENDIAN);
        for (int i=0; i<values.length; i++){
            buffer.putFloat(i*4,values[i]);
        }
        return buffer.array();
    }

    public static byte[] intArrayToByteArray(Object[] values){
        ByteBuffer buffer = ByteBuffer.allocate(values.length*4).order(ByteOrder.BIG_ENDIAN);
        for (int i=0; i<values.length; i++){
            buffer.putInt(i * 4, (Integer) values[i]);
        }
        return buffer.array();
    }

    public static float[] byteArrayToFloatArray(byte[]values){
        FloatBuffer buffer = ByteBuffer.wrap(values).order(ByteOrder.BIG_ENDIAN).asFloatBuffer();
        float[] array = new float[values.length/4];
        buffer.get(array);
        return array;
    }

    public static int[] byteArrayToIntArray(byte[]values){
        IntBuffer buffer = ByteBuffer.wrap(values).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        int[] array = new int[values.length/4];
        buffer.get(array);
        return array;
    }

    public static class MatrixWithHeader{
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
}