package org.wikibrain.cookbook.tsne;

import java.io.File;

import static org.wikibrain.cookbook.tsne.TSneDemo.saveFile;

/**
 * Created by Anja Beth Swoap on 6/6/16
 */

public class TsneWBData{


    public static void main(String [] args) {
        int initial_dims = 50;
        double perplexity = 20.0;
        double [][] X = MatrixUtils.simpleRead2DMatrix(new File("/Users/research/wikibrain/wikibrain-cookbook/results/kmeans-results/ordered_vectors.txt"), ",");
        System.out.println(MatrixOps.doubleArrayToPrintString(X, ", ", 50,10));
        TSne tsne = new FastTSne();
        double [][] Y = tsne.tsne(X, 2, initial_dims, perplexity, 1000);

        // Plot Y or save Y to file and plot with some other tool such as for instance R
        saveFile(new File("wikibrain-cookbook/results/tsne-results/tsne_clustered.txt"), MatrixOps.doubleArrayToString(Y));


    }

}
