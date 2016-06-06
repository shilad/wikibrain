package org.wikibrain.cookbook.kmeans;


import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.InstanceTools;
import net.sf.javaml.tools.data.FileHandler;

import org.math.plot.plots.ScatterPlot;
import org.wikibrain.cookbook.tsne.MatrixOps;
import org.wikibrain.cookbook.tsne.MatrixUtils;

import java.util.Iterator;


import java.io.File;
import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import static org.wikibrain.cookbook.tsne.TSneDemo.saveFile;


/**
 * Created by research on 6/3/16.
 */
public class KmeansWBSimple8000 {
    public static final int numClusters = 12;
    public static final int numIters = 100;



        public static void main(String [] args) throws IOException {

            double [][] vectors = MatrixUtils.simpleRead2DMatrix(new File("/Users/research/wikibrain/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/tsne/data/vecssmaller.txt"), ",");

            Dataset vectorData = new DefaultDataset();

            //establish file reader for article labels to attach to instances
            FileReader titleRdr = new FileReader("/Users/research/wikibrain/wikibrain-cookbook/src/main/java/org/wikibrain/cookbook/sr/names.txt");
            BufferedReader bufferedReader = new BufferedReader(titleRdr);

            for(int i = 0; i < vectors.length; i++){
                //read one line at a time from the file of article titles + use that as the second arg in the denseinstance constructor
                String title = bufferedReader.readLine();
                DenseInstance d = new DenseInstance(vectors[i], title);
                vectorData.add(d);
            }


            //cluster using kmeans
            KMeans k = new KMeans(numClusters, numIters);

            System.out.println("clustering...");
            Dataset[] clustered = k.cluster(vectorData);
            System.out.println("finished clustering.");

            //establish FileWriters for cluster labels and article titles
            BufferedWriter clusterfw = new BufferedWriter (new FileWriter(new File ("wikibrain-cookbook/results/kmeans-results/ordered_cluster_labels.txt")));
            BufferedWriter articlefw = new BufferedWriter(new FileWriter(new File("wikibrain-cookbook/results/kmeans-results/ordered_article_titles.txt")));

            clusterfw.append("this is a file");
            articlefw.append("this is a file");

            //establish 2D array to hold vectors in the correct order
            double[][] orderedVecs = new double[vectors.length][50];
            int twoDindex = 0;

            //establish InstanceTools to be able to write vectors properly
            InstanceTools itool = new InstanceTools();

            for(int i = 0; i < clustered.length; i++){
                //iterate through current Dataset and write each instance to the orderedVecs array, in order
                Dataset current = clustered[i];
                Iterator<Instance> iter = current.iterator();

                while(iter.hasNext()){
                    Instance currInst = iter.next();

                    //write current cluster label to file
                    clusterfw.append("cluster " + i + " \n");

                    //grab class (article title) from currInst and write to file
                    articlefw.append(currInst.classValue().toString()+ "\n");

                    //write vector array to orderedVecs
                    double[] vec = InstanceTools.array(currInst);
                    orderedVecs[twoDindex] = vec;

                    //increment twoDIndex to write to correct index in 2d array
                    twoDindex++;

                }
            }

            // write orderedVecs to file (use MatrixOps)
            saveFile(new File("wikibrain-cookbook/results/kmeans-results/ordered_vectors.txt"), MatrixOps.doubleArrayToString(orderedVecs));

            //close file reader
            bufferedReader.close();
            clusterfw.close();
            articlefw.close();

        }

}
