package org.wikapidia.sr.ensemble;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *@author Matt Lesicko
 */
public class LinearEnsemble implements Ensemble{
    private static final Logger LOG = Logger.getLogger(LinearEnsemble.class.getName());
    final int numMetrics;
    TDoubleArrayList simlarityCoefficients;
    TDoubleArrayList mostSimilarCoefficients;

    public LinearEnsemble(int numMetrics){
        this.numMetrics = numMetrics;
        simlarityCoefficients = new TDoubleArrayList();
        simlarityCoefficients.add(0.0);
        for (int i=0; i<numMetrics; i++){
            simlarityCoefficients.add(1.0 / numMetrics);
        }
        mostSimilarCoefficients = new TDoubleArrayList();
        mostSimilarCoefficients.add(0.0);
        for (int i=0; i<numMetrics; i++){
            mostSimilarCoefficients.add(1.0/numMetrics);
            mostSimilarCoefficients.add(0);
        }
    }

    public String  getName(){
        return "LinearEnsemble";
    }

    @Override
    public void trainSimilarity(List<EnsembleSim> simList) {
        if (simList.isEmpty()) {
            throw new IllegalArgumentException("no examples to train on!");
        }
        double[][] X = new double[simList.size()][numMetrics];
        double[] Y = new double[simList.size()];
        for (int i = 0; i<simList.size(); i++){
            Y[i]=simList.get(i).knownSim.similarity;
            for (int j=0; j<numMetrics; j++){
                X[i][j]=simList.get(i).getScores().get(j);
            }
        }
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(Y, X);

        simlarityCoefficients = new TDoubleArrayList(regression.estimateRegressionParameters());
        double pearson = Math.sqrt(regression.calculateRSquared());
        LOG.info("coefficients are " + simlarityCoefficients.toString());
        LOG.info("pearson for multiple regression is " + pearson);
    }

    @Override
    public void trainMostSimilar(List<EnsembleSim> simList) {
        if (simList.isEmpty()){
            throw new IllegalStateException("no examples to train on!");
        }
        double[][] X = new double[simList.size()][numMetrics*2];
        double[] Y = new double[simList.size()];
        for (int i=0; i<simList.size(); i++){
            Y[i]=simList.get(i).knownSim.similarity;
            for (int j=0; j<numMetrics; j++){
                X[i][2*j]= simList.get(i).getScores().get(j);
                X[i][2*j+1]= Math.log(simList.get(i).getRanks().get(j)+1);
            }
        }
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(Y,X);

        mostSimilarCoefficients = new TDoubleArrayList(regression.estimateRegressionParameters());
        double pearson = Math.sqrt(regression.calculateRSquared());
        LOG.info("coefficients are "+mostSimilarCoefficients.toString());
        LOG.info("pearson for multiple regression is "+pearson);
    }

    @Override
    public SRResult predictSimilarity(List<SRResult> scores) {
        if (scores.size()+1!= simlarityCoefficients.size()){
            throw new IllegalStateException();
        }
        double weightedScore = simlarityCoefficients.get(0);
        for (int i=0; i<scores.size(); i++){
            weightedScore+=(scores.get(i).getScore()* simlarityCoefficients.get(i+1));
        }
        return new SRResult(weightedScore);

    }

    @Override
    public SRResultList predictMostSimilar(List<SRResultList> scores, int maxResults) {
        if (2*scores.size()+1!= mostSimilarCoefficients.size()){
            throw new IllegalStateException();
        }
        TIntDoubleHashMap scoreMap = new TIntDoubleHashMap();
        int i =1;
        for (SRResultList resultList : scores){
            for (SRResult result : resultList){
                double value = result.getScore()*mostSimilarCoefficients.get(i);
                int rank = resultList.getIndexForId(result.getId())+1;
                value +=Math.log(rank)*mostSimilarCoefficients.get(i+1);
                scoreMap.adjustOrPutValue(result.getId(),value,value+mostSimilarCoefficients.get(0));
            }
            i+=2;
        }
        List<SRResult> resultList = new ArrayList<SRResult>();
        for (int id : scoreMap.keys()){
            resultList.add(new SRResult(id,scoreMap.get(id)));
        }
        Collections.sort(resultList);
        Collections.reverse(resultList);
        int size = maxResults>resultList.size()? resultList.size() : maxResults;
        SRResultList result = new SRResultList(size);
        for (i=0; i<size;i++){
            result.set(i,resultList.get(i));
        }
        return result;
    }

    @Override
    public void read(String path) throws IOException {
        try {
            ObjectInputStream oip = new ObjectInputStream(
                    new FileInputStream(path+"/ensemble/"+getName()+"/similarityCoefficients")
            );
            this.simlarityCoefficients = (TDoubleArrayList)oip.readObject();
            oip.close();
            oip = new ObjectInputStream(
                    new FileInputStream(path+"/ensemble/"+getName()+"/mostSimilarCoefficients")
            );
            this.mostSimilarCoefficients = (TDoubleArrayList)oip.readObject();
            oip.close();
        } catch (ClassNotFoundException e){
            throw new IOException("Malformed coefficient file(s)",e);
        }
    }

    @Override
    public void write(String path) throws IOException{
        ObjectOutputStream oop = new ObjectOutputStream(
                new FileOutputStream(path +"/ensemble/"+getName()+ "/similarityCoefficients")
        );
        oop.writeObject(simlarityCoefficients);
        oop.flush();
        oop.close();

        oop = new ObjectOutputStream(
                new FileOutputStream(path +"/ensemble/"+getName()+ "/mostSimilarCoefficients")
        );
        oop.writeObject(mostSimilarCoefficients);
        oop.flush();
        oop.close();
    }
}
