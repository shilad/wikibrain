package org.wikapidia.sr.ensemble;

import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.hibernate.criterion.Example;
import org.wikapidia.sr.SRResult;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *@author Matt Lesicko
 */
public class LinearEnsemble implements Ensemble{
    private static final Logger LOG = Logger.getLogger(LinearEnsemble.class.getName());
    final int numMetrics;
    TDoubleArrayList coefficients;

    public LinearEnsemble(int numMetrics){
        this.numMetrics = numMetrics;
        coefficients= new TDoubleArrayList();
        coefficients.add(0.0);
        for (int i=0; i<numMetrics; i++){
            coefficients.add(1/numMetrics);
        }
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

        coefficients = new TDoubleArrayList(regression.estimateRegressionParameters());
        double pearson = Math.sqrt(regression.calculateRSquared());
        LOG.info("coefficients are " + coefficients.toString());
        LOG.info("pearson for multiple regression is " + pearson);
    }

    @Override
    public void trainMostSimilar(List<EnsembleSim> simList) {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public SRResult predictSimilarity(List<SRResult> scores) {
        if (scores.size()+1!=coefficients.size()){
            throw new IllegalStateException();
        }
        double weightedScore = coefficients.get(0);
        for (int i=0; i<scores.size(); i++){
            weightedScore+=(scores.get(i).getScore()*coefficients.get(i+1));
        }
        return new SRResult(weightedScore);

    }

    @Override
    public SRResult predictMostSimilar(List<SRResult> scores) {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public void read(String file) {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(String file) {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }
}
