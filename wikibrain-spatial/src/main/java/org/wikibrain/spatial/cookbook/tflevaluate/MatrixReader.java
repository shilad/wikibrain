package org.wikibrain.spatial.cookbook.tflevaluate;

import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by harpa003 on 6/30/14.
 */
public class MatrixReader {

    public MatrixReader(){
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
            catch (FileNotFoundException e){
                System.out.println("File not found");
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


    }
}
