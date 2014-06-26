package org.wikibrain.spatial.cookbook.tflevaluate;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.omg.CORBA.Environment;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.*;
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
    private List<Integer> list;
    private float[][] matrix;
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
            this.geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        }catch(Exception e){
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

    }

    public static void main (String[] args){
        Env env = null;
        try{
            env = EnvBuilder.envFromArgs(args);
        }catch(ConfigurationException e){
            e.printStackTrace();
        }
        MatrixGenerator mg = new MatrixGenerator(env);
        MatrixWithHeader matrix = mg.generateSRMatrix();
        System.out.println("Finished generating matrix");
        mg.createMatrixFile("srmatrix",matrix);
        System.out.println("Finished writing matrix to file");
        MatrixWithHeader matrix2 = mg.loadMatrixFile("srmatrix",mg.geometries.keySet().size());
        System.out.println("Finished loading matrix");

        for (int i=0; i<matrix.matrix.length; i++) {
            if (!Arrays.equals(matrix.matrix[i], matrix2.matrix[i])){
                System.out.println("Unequal row "+i);
            }
        }

    }

    public MatrixWithHeader generateDistanceMatrix(){
        int size = geometries.size();
        float[][] matrix = new float[size][size];
        GeodeticCalculator calc = new GeodeticCalculator();
        List<Integer> list = Lists.newArrayList(geometries.keySet());
        for (int i = 0; i<size;i++){
            if (i%1000==0){
                LOG.log(Level.INFO, "Processed "+i+" geometries");
            }
            if (i==2000){
                break;
            }
            Point point1 = (Point) geometries.get(list.get(i));
            calc.setStartingGeographicPoint(point1.getX(),point1.getY());
            for (int j = 0; j<size;j++){
                Point point2 = (Point) geometries.get(list.get(j));
                calc.setDestinationGeographicPoint(point2.getX(),point2.getY());
                float distance = 0;
                try {
                    distance = (float) (calc.getOrthodromicDistance()/1000);
                } catch(ArithmeticException e){
                    try {
                        distance = (float) (DefaultEllipsoid.WGS84.orthodromicDistance(point1.getX(), point1.getY(), point2.getX(), point2.getY()) / 1000);
                    }catch(Exception e2){
//                        e2.printStackTrace();
                        distance = 20000;
                    }
                }
                matrix[i][j] = distance;
            }
        }
        return new MatrixWithHeader(matrix,list);
    }

    public MatrixWithHeader generateTopologicalMatrix(){
        int size = geometries.size();
        float[][] matrix = new float[size][size];
        List<Integer> list = Lists.newArrayList(geometries.keySet());

        // We will assume item ids are universal (because we think item_id in geometries are universal)

        //getTopologicalDistance(Geometry a, Integer itemIdA, Geometry b, Integer itemIdB, int k, String layerName, String refSysName)

        for (int i = 0; i<size;i++){
            if (i%1000==0){
                LOG.log(Level.INFO, "Processed "+i+" geometries");
            }

            int id1 = list.get(i);
            for (int j = 0; j<size;j++){

                float distance = 0;
                try {
                    int id2 = list.get(j);
                    distance = (float) dm.getTopologicalDistance(geometries.get(id1),id1,  geometries.get(id2),id2,  50, "wikidata", "earth");
                } catch(DaoException e){
                    e.printStackTrace();
                }
                matrix[i][j] = distance;
            }
        }
        return new MatrixWithHeader(matrix,list);
    }

    public MatrixWithHeader generateSRMatrix(){
        int size = geometries.size();
        matrix = new float[5000][5000];
        list = Lists.newArrayList(geometries.keySet());
        Collections.shuffle(list);
        list = list.subList(0,5000);
        System.out.println(list.size());


//        for (int i = 0; i<size;i++){
//            if (i%1==0){
//                LOG.log(Level.INFO, "Processed "+i+" geometries");
//            }
//            if (i==2000){
//                break;
//            }
//            int id1 = list.get(i);


            // feed it i,j
            // gets ids from list
            // stores in matrix[i][j]

            // newList<int[]>


            ParallelForEach.loop(list, new Procedure<Integer>() {
                @Override
                public void call(Integer id1) throws Exception {

                    int i = list.indexOf(id1);
                    for (int j=0; j<list.size(); j++) {


                        float distance = 0;
                        try {
                            SRResult similarity = sr.similarity(id1, list.get(j), false);
                            distance = (float) similarity.getScore();
                        } catch (DaoException e) {
                            e.printStackTrace();
                        }

                        matrix[i][j] = distance;
                    }
                    System.out.println("Processed row "+i);
                }
            });
//            for (int j = 0; j<size;j++){
//                float distance = 0;
//                try {
//                    int id2 = list.get(j);
//                    SRResult similarity = sr.similarity(id1,id2, false);
//                    distance = (float) similarity.getScore();
//                } catch(DaoException e){
//                    e.printStackTrace();
//                }
//                matrix[i][j] = distance;
//            }
//        }
        return new MatrixWithHeader(matrix,list);
    }

    public void createMatrixFile(String fileName, MatrixWithHeader matrixWithHeader){
        try {
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(fileName));

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

    public MatrixWithHeader loadMatrixFile(String fileName, int dimension){
        int byteDimension = dimension*4;
        long expectedFileSize = (dimension+1)*(long)byteDimension;

        File file = new File(fileName);
        if (file.length()!=expectedFileSize){
            System.out.println("Expected file size "+expectedFileSize+" but got "+file.length());
//            return null;
        }

        // what we read from the file
        byte[] result = new byte[byteDimension];
        float[][] matrix = new float[dimension][dimension];

        // list of integers
        List<Integer> ints = null;

        // mostly copied from http://www.javapractices.com/topic/TopicAction.do?Id=245
        try {
            InputStream input = null;
            try {
                long totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                int i = 0;
                // try to read all the bytes
                while(totalBytesRead < expectedFileSize){
                    long bytesRemaining = expectedFileSize - totalBytesRead;
                    if (bytesRemaining<dimension){
                        System.out.println("Unexpected array size: "+bytesRemaining+" bytesRemaining, although dimension is "+dimension);
                    }
                    //input.read() returns -1, 0, or more :
                    int bytesRead = input.read(result, 0, (int)Math.min(bytesRemaining,byteDimension));

                    if (bytesRead != byteDimension){
                        System.out.println("Read "+totalBytesRead+"/"+expectedFileSize);
                        System.out.println("Read wrong number of bytes--array will be wrong: read "+bytesRead+", wanted "+byteDimension);
                        System.exit(1);
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
        private Map<Integer, Integer> idToIndex;
        private List<Integer> idsInOrder;

        public MatrixWithHeader(float[][] matrix, List<Integer> idsInOrder){
            this.matrix = matrix;
            this.idsInOrder = idsInOrder;
            // generate map
            idToIndex = new HashMap<Integer, Integer>();
            for (int i=0; i<idsInOrder.size(); i++){
                idToIndex.put(idsInOrder.get(i),i);
            }
        }


    }
}
