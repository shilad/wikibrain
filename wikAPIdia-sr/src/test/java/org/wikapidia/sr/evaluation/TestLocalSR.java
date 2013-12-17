package org.wikapidia.sr.evaluation;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.KnownSim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class TestLocalSR implements LocalSRMetric {
    private Random random = new Random();

    // -1 values indicates failures
    private final Map<String, Double> estimates = new ConcurrentHashMap<String, Double>();

    @Override
    public String getName() {
        return "testMetric";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        return null;
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException {
        Double val;
        if (random.nextDouble() < 0.05) {
            val = -1.0;
        } else if (random.nextDouble() < 0.05) {
            val = Double.POSITIVE_INFINITY;
        } else if (random.nextDouble() < 0.05) {
            val = Double.NaN;
        } else {
            val = random.nextDouble();
        }
//        System.out.println(phrase1);
//        System.out.println(phrase2);
//        System.out.println(val);
        estimates.put(phrase1 + "," + phrase2, val);
        if (val < 0) {
            throw new DaoException("fake exception");
        } else {
            return new SRResult(val);
        }
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException {
        SRResultList result = new SRResultList(10);
        // TODO: figure out what this should return and test it.
        return result;
    }

    @Override
    public void write(String path) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void read(String path) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainDefaultSimilarity(Dataset dataset) throws DaoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainDefaultMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean similarityIsTrained() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean mostSimilarIsTrained() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDefaultMostSimilarNormalizer(Normalizer n) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDefaultSimilarityNormalizer(Normalizer defaultSimilarityNormalizer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setMostSimilarNormalizer(Normalizer n, Language l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSimilarityNormalizer(Normalizer n, Language l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds, Language language) throws DaoException {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(String[] rowPhrases, String[] colPhrases, Language language) throws DaoException {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(int[] ids, Language language) throws DaoException {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(String[] phrases, Language language) throws DaoException {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void readCosimilarity(String path, LanguageSet languages) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public TDoubleList getActual(List<KnownSim> gold) {
        TDoubleList actual = new TDoubleArrayList();
        for (KnownSim ks : gold) {
            Double value = estimates.get(ks.phrase1 + "," + ks.phrase2);
            if (value >= 0 && !value.isNaN() && !value.isInfinite()) {
                actual.add(ks.similarity);
            }
        }
        return actual;
    }

    public TDoubleList getEstimated(List<KnownSim> gold) {
        TDoubleList estimated = new TDoubleArrayList();
        for (KnownSim ks : gold) {
            Double value = estimates.get(ks.phrase1 + "," + ks.phrase2);
            if (value >= 0 && !value.isNaN() && !value.isInfinite()) {
                estimated.add(value);
            }
        }
        return estimated;
    }

    public int getMissing() {
        int n = 0;
        for (Double value : estimates.values()) {
            if (value >= 0 && value.isInfinite() || value.isNaN()) {
                n++;
            }
        }
        return n;
    }

    public int getFailed() {
        int n = 0;
        for (Double value : estimates.values()) {
            if (value < 0) {
                n++;
            }
        }
        return n;
    }

    public int getSuccessful() {
        return estimates.size() - getMissing() - getFailed();
    }

    public int getTotal() {
        return estimates.size();
    }

    public static class Factory implements LocalSRFactory {
        public List<TestLocalSR> metrics = new ArrayList<TestLocalSR>();
        @Override
        public LocalSRMetric create() {
            TestLocalSR sr = new TestLocalSR();
            metrics.add(sr);
            return sr;
        }

        @Override
        public String describeDisambiguator() {
            return "thisIsTheDisambiguator";
        }

        @Override
        public String describeMetric() {
            return "thisIsTheMetric";
        }

        @Override
        public String getName() {
            return "testMetric";
        }
    }
}
