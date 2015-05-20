package org.wikibrain.sr.ensemble;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *@author Matt Lesicko
 */
public class CorrelationEnsemble implements Ensemble{
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationEnsemble.class);
    final int numMetrics;
    private final int numTrainingCandidateArticles;
    TDoubleArrayList simlarityCoefficients;
    TDoubleArrayList mostSimilarCoefficients;
    Interpolator similarityInterpolator;
    Interpolator mostSimilarInterpolator;

    public CorrelationEnsemble(int numMetrics, int numTrainingCandidateArticles){
        this.numTrainingCandidateArticles = numTrainingCandidateArticles;
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
        similarityInterpolator = new Interpolator(numMetrics);
        mostSimilarInterpolator = new Interpolator(numMetrics);
    }

    public String  getName(){
        return "LinearEnsemble";
    }

    @Override
    public void trainSimilarity(List<EnsembleSim> simList) {
        if (simList.isEmpty()) {
            throw new IllegalArgumentException("no examples to train on!");
        }
        similarityInterpolator.trainSimilarity(simList);
        double[][] X = new double[simList.size()][numMetrics];
        double[] Y = new double[simList.size()];
        for (int i = 0; i<simList.size(); i++){
            Y[i]=simList.get(i).knownSim.similarity;
            EnsembleSim es = similarityInterpolator.interpolate(simList.get(i));
            for (int j=0; j<numMetrics; j++){
                X[i][j]=es.getScores().get(j);
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
        mostSimilarInterpolator.trainMostSimilar(simList);

        // Remove things that have no observed metrics
        List<EnsembleSim> pruned = new ArrayList<EnsembleSim>();
        for (EnsembleSim es : simList) {
            if (es != null && es.getNumMetricsWithScore() > 0) {
                pruned.add(mostSimilarInterpolator.interpolate(es));
            }
        }

        double [][] X = new double[2][pruned.size()];
        double[] Y = new double[pruned.size()];
        mostSimilarCoefficients = new TDoubleArrayList();
        for (int i = 0; i < numMetrics; i++) {
            for (int j=0; j<pruned.size(); j++){
                EnsembleSim es = pruned.get(j);
                Y[j]=es.knownSim.similarity;
                X[0][j]= es.getScores().get(i);
                X[1][j]= Math.log(es.getRanks().get(i)+1);
            }
            double pearson1 = new PearsonsCorrelation().correlation(X[0], Y);
            double pearson2 = new PearsonsCorrelation().correlation(X[1], Y);
            double dev1 = Math.sqrt(StatUtils.variance(X[0]));
            double dev2 = Math.sqrt(StatUtils.variance(X[1]));

            mostSimilarCoefficients.add(pearson1 / dev1);
            mostSimilarCoefficients.add(pearson2 / dev2);
        }

        LOG.info("coefficients are "+mostSimilarCoefficients.toString());
    }

    @Override
    public SRResult predictSimilarity(List<SRResult> scores) {
        if (scores.size()+1!= simlarityCoefficients.size()){
            throw new IllegalStateException();
        }
        double weightedScore = simlarityCoefficients.get(0);
        for (int i=0; i<scores.size(); i++){
            double s = scores.get(i) == null ? Double.NaN : scores.get(i).getScore();
            if (Double.isNaN(s) || Double.isInfinite(s)) {
                s = similarityInterpolator.getInterpolatedScore(i);
            }
            weightedScore+=(s * simlarityCoefficients.get(i+1));
        }
        return new SRResult(weightedScore);

    }

    public static boolean debug = false;
    @Override
    public SRResultList predictMostSimilar(List<SRResultList> scores, int maxResults, TIntSet validIds) {
        if (2*scores.size() != mostSimilarCoefficients.size()){
            throw new IllegalStateException();
        }
        TIntSet allIds = new TIntHashSet();    // ids returned by at least one metric
        for (SRResultList resultList : scores){
            if (resultList != null) {
                for (SRResult result : resultList){
                    allIds.add(result.getId());
                }
            }
        }

        TIntDoubleHashMap scoreMap = new TIntDoubleHashMap();
        int i = 0;
        for (SRResultList resultList : scores){
            TIntSet unknownIds = new TIntHashSet(allIds);
            double c1 = mostSimilarCoefficients.get(i);     // score coeff
            double c2 = mostSimilarCoefficients.get(i + 1);   // rank coefficient

            // expand or contract ranks proportionately to number of articles we see
            double k = 1.0;
            if (validIds != null) {
                k = 1.0 * numTrainingCandidateArticles / validIds.size();
            }
            int interpolatedRank = (int) (mostSimilarInterpolator.getInterpolatedRank(i / 2) * k);

            if (resultList != null) {
                for (int j = 0; j < resultList.numDocs(); j++) {
                    int rank = (int) ((j + 1) * k);
                    SRResult result = resultList.get(j);
                    unknownIds.remove(result.getId());
                    double value = c1 * result.getScore() + c2 * Math.log(rank);
                    if (debug) {
                        System.err.format("%s %d. %.3f (id=%d), computing %.3f * %.3f + %.3f * (log(%d) = %.3f)\n",
                                "m" + i, j, value, result.getId(),
                                c1, result.getScore(), c2, rank, Math.log(rank));
                    }
                    scoreMap.adjustOrPutValue(result.getId(), value, value);
                }
                interpolatedRank = (int) Math.max(interpolatedRank, k * resultList.numDocs() * 5 / 4);
            }

            // interpolate scores for unknown ids
            double value = c1 * mostSimilarInterpolator.getInterpolatedScore(i/2)
                         + c2 * Math.log(interpolatedRank);
            for (int id : unknownIds.toArray()) {
                scoreMap.adjustOrPutValue(id, value, value);
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
        File dir = FileUtils.getFile(path, "ensemble", getName());
        if (!dir.isDirectory()) {
            return;
        }
        try {
            ObjectInputStream oip = new ObjectInputStream(
                    new FileInputStream(new File(dir, "similarityCoefficients")));
            this.simlarityCoefficients = (TDoubleArrayList)oip.readObject();
            oip.close();
            oip = new ObjectInputStream(
                    new FileInputStream(new File(dir, "mostSimilarCoefficients")));
            this.mostSimilarCoefficients = (TDoubleArrayList)oip.readObject();
            oip.close();
            oip = new ObjectInputStream(
                    new FileInputStream(new File(dir, "similarityInterpolator")));
            this.similarityInterpolator = (Interpolator) oip.readObject();
            oip.close();
            oip = new ObjectInputStream(
                    new FileInputStream(new File(dir, "mostSimilarInterpolator")));
            this.mostSimilarInterpolator = (Interpolator) oip.readObject();
            oip.close();
        } catch (ClassNotFoundException e){
            throw new IOException("Malformed coefficient file(s)",e);
        }
    }

    @Override
    public void write(String path) throws IOException{
        File dir = FileUtils.getFile(path, "ensemble", getName());
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        ObjectOutputStream oop = new ObjectOutputStream(
                new FileOutputStream(new File(dir, "similarityCoefficients")));
        oop.writeObject(simlarityCoefficients);
        oop.flush();
        oop.close();

        oop = new ObjectOutputStream(
                new FileOutputStream(new File(dir, "mostSimilarCoefficients")));
        oop.writeObject(mostSimilarCoefficients);
        oop.flush();
        oop.close();

        oop = new ObjectOutputStream(
                new FileOutputStream(new File(dir, "similarityInterpolator")));
        oop.writeObject(similarityInterpolator);
        oop.flush();
        oop.close();

        oop = new ObjectOutputStream(
                new FileOutputStream(new File(dir, "mostSimilarInterpolator")));
        oop.writeObject(mostSimilarInterpolator);
        oop.flush();
        oop.close();
    }
}
