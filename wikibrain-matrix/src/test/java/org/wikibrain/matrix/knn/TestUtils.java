package org.wikibrain.matrix.knn;

import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;
import org.wikibrain.matrix.DenseMatrixWriter;
import org.wikibrain.matrix.ValueConf;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class TestUtils {
    public static DenseMatrix createMatrix(int rows, int cols) throws IOException {
        File tmp = File.createTempFile("knnfinder", ".matrix");
        tmp.delete();
        ValueConf vconf = new ValueConf();
        int [] colIds = new int[cols];
        for (int i= 0 ; i < cols; i++) { colIds[i] = i; }
        DenseMatrixWriter writer = new DenseMatrixWriter(tmp, vconf);
        for (int i = 0; i < rows; i++) {
            writer.writeRow(new DenseMatrixRow(vconf, i, colIds, randomVector(cols)));
        }
        writer.finish();
        tmp.deleteOnExit();
        return new DenseMatrix(tmp);
    }

    static float[] randomVector(int cols) {
        Random rand = new Random();
        double norm = 0.0;
        float [] vals = new float[cols];
        for (int j = 0; j < cols; j++) {
            vals[j] = rand.nextFloat();
            norm += vals[j] * vals[j];
        }
        norm = Math.sqrt(norm) + 0.00001;
        for (int i = 0; i < cols; i++) {
            vals[i] /= norm;
        }
        return vals;
    }
}
