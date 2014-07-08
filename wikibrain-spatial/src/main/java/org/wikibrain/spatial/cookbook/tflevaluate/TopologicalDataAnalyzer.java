package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by maixa001 on 7/8/14.
 */
public class TopologicalDataAnalyzer {


    private int size;
    private List<Integer> list;
    private float[][] matrix;
    private int maxLevel;


    public TopologicalDataAnalyzer(Env env, int size){
        SpatialDataDao sdDao = null;
        this.size = size;
        Map<Integer,Geometry> geometries;
        list = new ArrayList<Integer>();
        Configurator c = env.getConfigurator();
        try {
            sdDao= c.get(SpatialDataDao.class);
            geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
            List<Integer> temp = new ArrayList<Integer>();
            temp.addAll(geometries.keySet());
            System.out.println(temp.size());
            int count = 0;
            while (count<size){
                int current = (int) (Math.random()*temp.size());
                if (!list.contains(temp.get(current))){
                    list.add(temp.get(current));
                    count++;
                }
            }
            MatrixGenerator mg = new MatrixGenerator(env);
            mg.pageHitList = list;
            matrix = mg.generateSRMatrix().matrix;
            for (int i = 0; i<size; i++){
                for (int j = 0; j<size; j++){
                    if (i!=j) {
                        matrix[i][j] = 1 / matrix[i][j];
                    }
                }
            }
        }catch(DaoException e){
            e.printStackTrace();
        }catch (ConfigurationException e){
            e.printStackTrace();
        }
    }

    public void generateFile(){
        try{
            PrintWriter pw = new PrintWriter(new File("input.txt"));
            pw.println("1");
            for (Integer item: list){
                pw.println(0 + " " + item + " " + "0");
            }
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (i!=j)
                    pw.println("1 " + list.get(i) + " " + list.get(j) + " " + matrix[i][j]);
                }
            }
            for (int i = 0; i<size; i++){
                for (int j = 0; j<size; j++){
                    if (i!=j) {
                        for (int k = 0; k < size; k++) {
                            if (k!=i && k != j){
                                pw.println("2 "+list.get(i)+" "+list.get(j)+" "+list.get(k)+" "+Math.max(Math.max(matrix[i][j],matrix[j][k]), matrix[i][k]));
                            }
                        }
                    }
                }
            }

            pw.close();
        } catch (IOException e){
            System.out.println("cannot create file");
        }
    }

    public static void main(String[] args) throws ConfigurationException {
        Env env = EnvBuilder.envFromArgs(args);
        TopologicalDataAnalyzer tda = new TopologicalDataAnalyzer(env,100);
        tda.generateFile();
    }


}
