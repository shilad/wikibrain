package org.wikapidia.sr.evaluation;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.normalize.Normalizer;

import java.io.File;
import java.io.IOException;

/**
 * A factory that uses a pretrained metric.
 * Any calls to train, read, or write the metric are ignored.
 *
 * @author Shilad Sen
 */
public class PretrainedSRFactory implements MonolingualSRFactory {
    private final MonolingualSRMetric metric;

    public PretrainedSRFactory(MonolingualSRMetric metric) {
        this.metric = metric;
    }
    @Override
    public MonolingualSRMetric create() {
        return new PretrainedMetric(metric);
    }

    @Override
    public String describeDisambiguator() {
        return "pretrained";
    }

    @Override
    public String describeMetric() {
        return "pretrained-" + metric.getName();
    }

    @Override
    public String getName() {
        return metric.getName();
    }

    public static class PretrainedMetric implements MonolingualSRMetric {
        private final MonolingualSRMetric delegate;

        public PretrainedMetric(MonolingualSRMetric delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Language getLanguage() {
            return delegate.getLanguage();
        }

        @Override
        public File getDataDir() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void setDataDir(File dir) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
            return delegate.similarity(pageId1, pageId2, explanations);
        }

        @Override
        public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
            return delegate.similarity(phrase1, phrase2, explanations);
        }

        @Override
        public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
            return delegate.mostSimilar(pageId, maxResults);
        }

        @Override
        public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
            return delegate.mostSimilar(pageId, maxResults, validIds);
        }

        @Override
        public SRResultList mostSimilar(String phrase, int maxResults) throws DaoException {
            return delegate.mostSimilar(phrase, maxResults);
        }

        @Override
        public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
            return delegate.mostSimilar(phrase, maxResults, validIds);
        }

        @Override
        public boolean similarityIsTrained() {
            return delegate.similarityIsTrained();
        }

        @Override
        public boolean mostSimilarIsTrained() {
            return delegate.mostSimilarIsTrained();
        }

        @Override
        public void setMostSimilarNormalizer(Normalizer n) {
            delegate.setMostSimilarNormalizer(n);
        }

        @Override
        public void setSimilarityNormalizer(Normalizer defaultSimilarityNormalizer) {
            delegate.setSimilarityNormalizer(defaultSimilarityNormalizer);
        }

        @Override
        public TIntDoubleMap getVector(int id) throws DaoException {
            return delegate.getVector(id);
        }

        @Override
        public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds) throws DaoException {
            return delegate.cosimilarity(wpRowIds, wpColIds);
        }

        @Override
        public double[][] cosimilarity(String[] rowPhrases, String[] colPhrases) throws DaoException {
            return delegate.cosimilarity(rowPhrases, colPhrases);
        }

        @Override
        public double[][] cosimilarity(int[] ids) throws DaoException {
            return delegate.cosimilarity(ids);
        }

        @Override
        public double[][] cosimilarity(String[] phrases) throws DaoException {
            return delegate.cosimilarity(phrases);
        }

        @Override
        public Normalizer getMostSimilarNormalizer() {
            return delegate.getMostSimilarNormalizer();
        }

        @Override
        public Normalizer getSimilarityNormalizer() {
            return delegate.getSimilarityNormalizer();
        }

        @Override
        public void writeCosimilarity(int maxHits) throws IOException, DaoException, WikapidiaException {}
        @Override
        public void writeCosimilarity(int maxHits, TIntSet rowIds, TIntSet colIds) throws IOException, DaoException, WikapidiaException {}
        @Override
        public void write() throws IOException {}
        @Override
        public void read() throws IOException {}
        @Override
        public void trainSimilarity(Dataset dataset) throws DaoException {}
        @Override
        public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {}


    }
}
