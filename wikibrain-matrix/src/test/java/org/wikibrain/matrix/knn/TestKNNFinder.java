package org.wikibrain.matrix.knn;

import org.junit.Test;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;
import org.wikibrain.matrix.DenseMatrixWriter;
import org.wikibrain.matrix.ValueConf;
import org.wikibrain.matrix.knn.KmeansKNNFinder;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class TestKNNFinder {

    @Test
    public void testBuild() throws IOException {
        DenseMatrix matrix = createMatrix(100000, 20);
        KmeansKNNFinder finder = new KmeansKNNFinder(matrix);
        finder.build();
    }

    private static DenseMatrix createMatrix(int rows, int cols) throws IOException {
        File tmp = File.createTempFile("knnfinder", ".matrix");
        tmp.delete();
        ValueConf vconf = new ValueConf();
        int [] colIds = new int[cols];
        for (int i= 0 ; i < cols; i++) { colIds[i] = i; }
        DenseMatrixWriter writer = new DenseMatrixWriter(tmp, vconf);
        float [] vals = new float[cols];
        Random rand = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                vals[j] = rand.nextFloat();
            }
            writer.writeRow(new DenseMatrixRow(vconf, i, colIds, vals));
        }
        writer.finish();
        tmp.deleteOnExit();
        return new DenseMatrix(tmp);
    }
}
