package org.wikibrain.sr.normalize;

import com.typesafe.config.Config;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.sr.SRResultList;

import java.text.DecimalFormat;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankAndScoreNormalizer extends BaseNormalizer {
    private static Logger LOG = LoggerFactory.getLogger(RankAndScoreNormalizer.class);

    private double intercept;
    private double rankCoeff;
    private double scoreCoeff;
    private boolean logTransform = false;

    // temporary accumulators that feed into regression
    private transient TIntArrayList ranks = new TIntArrayList();
    private transient TDoubleArrayList scores = new TDoubleArrayList();
    private transient TDoubleArrayList ys = new TDoubleArrayList();

    @Override
    public void reset() {
        ranks.clear();
        scores.clear();
        ys.clear();
    }

    @Override
    public void observe(SRResultList list, int index, double y) {
        if (index >= 0) {
            double score = list.getScore(index);
            if (!Double.isNaN(score) && !Double.isInfinite(score)) {
                synchronized (ranks) {
                    ranks.add(index);
                    scores.add(score);
                    ys.add(y);
                }
            }
        }
        super.observe(list, index, y);
    }

    public void setLogTransform(boolean logTransform) {
        this.logTransform = logTransform;
    }

    @Override
    public void observationsFinished() {
        double Y[] = ys.toArray();
        double X[][] = new double[Y.length][2];
        for (int i = 0; i < Y.length; i++) {
            X[i][0] = Math.log(1 + ranks.get(i));
            X[i][1] = logIfNecessary(scores.get(i));
        }
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(Y, X);
        double [] params = regression.estimateRegressionParameters();
        intercept = params[0];
        rankCoeff = params[1];
        scoreCoeff = params[2];
        super.observationsFinished();
        LOG.info("trained model on " + X.length + " observations: " + dump() + " with R-squared " + regression.calculateRSquared());
    }

    @Override
    public SRResultList normalize(SRResultList list) {
        SRResultList normalized = new SRResultList(list.numDocs());
        normalized.setMissingScore(missingMean);
        for (int i = 0; i < list.numDocs(); i++) {
            double s = logIfNecessary(list.getScore(i));
            double score = intercept + rankCoeff * Math.log(i + 1) + scoreCoeff * s;
            normalized.set(i, list.getId(i), score);
        }
        return normalized;

    }

    private double logIfNecessary(double x) {
        return logTransform ? Math.log(1 + x - min) : x;
    }

    @Override
    public double normalize(double x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String dump() {
        DecimalFormat df = new DecimalFormat("#.###");
        return (
                df.format(rankCoeff) + "*log(1+rank) + " +
                df.format(scoreCoeff) + "*score + " +
                df.format(intercept)
            );
    }

    @Override
    public String toString() {
        return "Rank and score normalizer: " + dump();
    }

    public static class Provider extends org.wikibrain.conf.Provider<RankAndScoreNormalizer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return Normalizer.class;
        }

        @Override
        public String getPath() {
            return "sr.normalizer";
        }

        @Override
        public Scope getScope() {
            return Scope.INSTANCE;
        }

        @Override
        public RankAndScoreNormalizer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("rank")) {
                return null;
            }

            return new RankAndScoreNormalizer();
        }

    }
}
