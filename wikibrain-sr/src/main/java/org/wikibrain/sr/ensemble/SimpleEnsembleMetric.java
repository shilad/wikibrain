package org.wikibrain.sr.ensemble;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.normalize.Normalizer;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * A simple linear ensemble that requires no training or configuration.
 *
 * Since this metric is zero-training / zero-configuration, all the training,
 * normalization, and I/O methods are trivial no-op implementations.
 *
 * @author Shilad Sen
 */
public class SimpleEnsembleMetric implements SRMetric {
    private class SubMetric {
        SRMetric metric;
        double coefficient;
    }

    private static final Logger LOG = LoggerFactory.getLogger(SimpleEnsembleMetric.class);
    private final String name;
    private final Language language;
    private SubMetric metrics[];
    private boolean trainSubmetrics = true;
    private double numCandidateMultiplier = 2.0;


    public SimpleEnsembleMetric(String name, Language language, List<SRMetric> metrics, List<Double> coefficients){
        if (metrics.size() != coefficients.size()) {
            throw new IllegalArgumentException();
        }
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("Must supply at least one metric to the simple ensemble.");
        }
        this.metrics=new SubMetric[metrics.size()];
        for (int i =0 ; i < metrics.size(); i++) {
            this.metrics[i] = new SubMetric();
            this.metrics[i].metric = metrics.get(i);
            this.metrics[i].coefficient = coefficients.get(i);
        }
        this.name = name;
        this.language = language;
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        // TODO: Handle explanations
        double sum = 0.0;
        double divisor = 0.0;
        for (SubMetric m : metrics) {
            SRResult r = m.metric.similarity(pageId1, pageId2, false);
            if (r != null && r.isValid()) {
                sum += m.coefficient * r.getScore();
                divisor += m.coefficient;
            }
        }
        return new SRResult((divisor > 0) ? (sum / divisor) : Double.NaN);
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        double sum = 0.0;
        double divisor = 0.0;
        for (SubMetric m : metrics) {
            SRResult r = m.metric.similarity(phrase1, phrase2, false);
            if (r != null && r.isValid()) {
                sum += m.coefficient * r.getScore();
                divisor += m.coefficient;
            }
        }
        return new SRResult((divisor > 0) ? (sum / divisor) : Double.NaN);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
        return mostSimilar(pageId, maxResults, null);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        TIntSet candidateSet = new TIntHashSet();
        for (SubMetric m : metrics) {
            SRResultList rl = m.metric.mostSimilar(pageId, (int)(maxResults * numCandidateMultiplier), validIds);
            if (rl != null) {
                for (SRResult r : rl) {
                    candidateSet.add(r.getId());
                }
            }
        }
        int candidates[] = candidateSet.toArray();
        double cosims[][] = cosimilarity(new int[]{pageId}, candidates);
        Leaderboard top = new Leaderboard(maxResults);
        for (int i = 0; i < candidates.length; i++) {
            top.tallyScore(candidates[i], cosims[0][i]);
        }
        return top.getTop();
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults) throws DaoException {
        return mostSimilar(phrase, maxResults, null);
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        TIntSet candidateSet = new TIntHashSet();
        for (SubMetric m : metrics) {
            SRResultList rl = m.metric.mostSimilar(phrase, (int) (maxResults * numCandidateMultiplier), validIds);
            if (rl != null) {
                for (SRResult r : rl) {
                    candidateSet.add(r.getId());
                }
            }
        }
        // Hack: because there's no way to compare a phrase query and articles,
        // we need to re-ask mostSimilar with the specified candidate list.
        TIntDoubleMap scores = new TIntDoubleHashMap();
        for (SubMetric m : metrics) {
            // Hack: The bottom 20% all get the same (lowest) score.
            SRResultList rl = m.metric.mostSimilar(phrase,
                    (int) Math.ceil(candidateSet.size() * 0.8),
                    candidateSet);
            if (rl != null && rl.numDocs() > 0) {
                TIntFloatMap subscores = rl.asTroveMap();
                double minScore = rl.getScore(rl.numDocs() - 1) * 0.99;
                for (int id : subscores.keys()) {
                    double s = minScore;
                    if (subscores.containsKey(id)) {
                        s = subscores.get(id);
                        if (Double.isInfinite(s) || Double.isNaN(s)) {
                            s = minScore;
                        }
                    }
                    s *= m.coefficient;
                    scores.adjustOrPutValue(id, s, s);
                }
            }
        }
        final Leaderboard top = new Leaderboard(maxResults);
        scores.forEachEntry(new TIntDoubleProcedure() {
            @Override
            public boolean execute(int id, double score) {
                top.tallyScore(id, score);
                return true;
            }
        });
        return top.getTop();
    }

    @Override
    public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds) throws DaoException {
        double result[][] = new double[wpRowIds.length][wpColIds.length];
        for (SubMetric m : metrics) {
            double r[][] = m.metric.cosimilarity(wpRowIds, wpColIds);
            for (int i = 0; i < wpRowIds.length; i++) {
                for (int j = 0; j < wpColIds.length; j++) {
                    double s = r[i][j];
                    if (!Double.isNaN(s) && !Double.isInfinite(s)) {
                        result[i][j] += s * m.coefficient;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public double[][] cosimilarity(String[] rowPhrases, String[] colPhrases) throws DaoException {
        double result[][] = new double[rowPhrases.length][colPhrases.length];
        for (SubMetric m : metrics) {
            double r[][] = m.metric.cosimilarity(rowPhrases, colPhrases);
            for (int i = 0; i < rowPhrases.length; i++) {
                for (int j = 0; j < colPhrases.length; j++) {
                    double s = r[i][j];
                    if (!Double.isNaN(s) && !Double.isInfinite(s)) {
                        result[i][j] += s * m.coefficient;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public double[][] cosimilarity(int[] ids) throws DaoException {
        return cosimilarity(ids, ids);
    }

    @Override
    public double[][] cosimilarity(String[] phrases) throws DaoException {
        return cosimilarity(phrases, phrases);
    }

    public void setTrainSubmetrics(boolean trainSubmetrics) {
        this.trainSubmetrics = trainSubmetrics;
    }

    @Override
    public Normalizer getMostSimilarNormalizer() { return null; }

    @Override
    public void setMostSimilarNormalizer(Normalizer n) {}

    @Override
    public Normalizer getSimilarityNormalizer() { return null; }

    @Override
    public void setSimilarityNormalizer(Normalizer n) {}

    @Override
    public File getDataDir() { return null; }

    @Override
    public void setDataDir(File dir) {}


    @Override
    public void write() throws IOException {}

    @Override
    public void read() throws IOException {}


    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        if (trainSubmetrics) {
            for (SubMetric m : metrics) m.metric.trainSimilarity(dataset);
        }

    }

    @Override
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
        if (trainSubmetrics) {
            for (SubMetric m : metrics) m.metric.trainMostSimilar(dataset, numResults, validIds);
        }
    }

    @Override
    public boolean similarityIsTrained() {
        if (trainSubmetrics) {
            for (SubMetric m : metrics) if (!m.metric.similarityIsTrained()) return false;
        }
        return true;
    }

    @Override
    public boolean mostSimilarIsTrained() {
        if (trainSubmetrics) {
            for (SubMetric m : metrics) if (!m.metric.mostSimilarIsTrained()) return false;
        }
        return true;
    }

    public void setNumCandidateMultiplier(double numCandidateMultiplier) {
        this.numCandidateMultiplier = numCandidateMultiplier;
    }

    public static class Provider extends org.wikibrain.conf.Provider<SRMetric>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException{
            if (!config.getString("type").equals("simple-ensemble")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            if (!config.hasPath("metrics")){
                throw new ConfigurationException("Ensemble metric has no base metrics to use.");
            }
            List<String> metricNames = config.getStringList("metrics");
            List<Double> allCoefficients = config.getDoubleList("coefficients");

            List<SRMetric> metrics = new ArrayList<SRMetric>();
            List<Double> activeCoefficients = new ArrayList<Double>();

            for (int i = 0; i < metricNames.size(); i++) {
                try {
                    metrics.add(getConfigurator().get(SRMetric.class, metricNames.get(i),

                            "language", language.getLangCode()));
                    activeCoefficients.add(allCoefficients.get(i));
                } catch (Exception e) {
                    LOG.error("Loading of metric " + metricNames.get(i) + " failed. Skipping it! Error:", e);
                }
            }

            return new SimpleEnsembleMetric(name, language, metrics, activeCoefficients);
        }
    }
}
