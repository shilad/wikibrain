package org.wikibrain.cookbook.tsne;

/**
 * Created by research on 6/2/16.
 */

import org.math.plot.FrameView;
import org.math.plot.Plot2DPanel;

import java.io.File;

import static org.wikibrain.cookbook.tsne.TSneDemo.saveFile;





/**
 * Created by research on 6/2/16.
 */



public class TsneDemoMnistTest {
    public static void main(String [] args) {
        int initial_dims = 50;
        double perplexity = 20.0;
        double [][] X = MatrixUtils.simpleRead2DMatrix(new File("tsne-demos/src/main/resources/datasets/mnist250_X.txt"), ",");
        System.out.println(MatrixOps.doubleArrayToPrintString(X, ", ", 50,10));
        TSne tsne = new FastTSne();
        double [][] Y = tsne.tsne(X, 2, initial_dims, perplexity);

        // Plot Y or save Y to file and plot with some other tool such as for instance R
        saveFile(new File("Java-tsne-resultmnist.txt"), MatrixOps.doubleArrayToString(Y));
/*
        //String [] labels = MatrixUtils.simpleReadLines(new File("tsne-demos/src/main/resources/datasets/nnist250_labels.txt"));
        //for (int i = 0; i < labels.length; i++) {
            labels[i] = labels[i].trim().substring(0, 1);
        }

        Plot2DPanel plot = new Plot2DPanel();

        ColoredScatterPlot mnistPlot = new ColoredScatterPlot("mnist", Y, labels);
        plot.plotCanvas.setNotable(true);
        plot.plotCanvas.setNoteCoords(true);
        plot.plotCanvas.addPlot(mnistPlot);

        FrameView plotframe = new FrameView(plot);
        plotframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        plotframe.setVisible(true);
*/
    }
}


