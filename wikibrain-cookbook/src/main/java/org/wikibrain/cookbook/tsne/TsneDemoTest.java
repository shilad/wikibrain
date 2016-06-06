package org.wikibrain.cookbook.tsne;

import org.math.plot.FrameView;
import org.math.plot.Plot2DPanel;
//import org.math.plot.plots.ColoredScatterPlot;

import javax.swing.*;
import java.io.File;

import static org.wikibrain.cookbook.tsne.TSneDemo.saveFile;


/**
 * Created by research on 6/2/16.
 */



    public class TsneDemoTest {
        public static void main(String [] args) {
            int initial_dims = 50;
            double perplexity = 20.0;
            double [][] X = MatrixUtils.simpleRead2DMatrix(new File("/Users/research/Desktop/wikibrain/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/tsne/data/testing.txt"), ", ");
            System.out.println(MatrixOps.doubleArrayToPrintString(X, ", ", 50,10));
            TSne tsne = new FastTSne();
            double [][] Y = tsne.tsne(X, 2, initial_dims, perplexity);

            // Plot Y or save Y to file and plot with some other tool such as for instance R
            saveFile(new File("Java-tsne-testing-results.txt"), MatrixOps.doubleArrayToString(Y));

            /* String [] labels = MatrixUtils.simpleReadLines(new File("tsne-demos/src/main/resources/datasets/iris_X_labels.txt"));
            for (int i = 0; i < labels.length; i++) {
                labels[i] = labels[i].trim().substring(0, 3);
            }

            Plot2DPanel plot = new Plot2DPanel();

            ColoredScatterPlot setosaPlot = new ColoredScatterPlot("setosa", Y, labels);
            plot.plotCanvas.setNotable(true);
            plot.plotCanvas.setNoteCoords(true);
            plot.plotCanvas.addPlot(setosaPlot);

            FrameView plotframe = new FrameView(plot);
            plotframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            plotframe.setVisible(true);

            */

        }
    }

