package org.wikibrain.sr.evaluation;

import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.normalize.Normalizer;

import java.io.File;
import java.io.IOException;

/**
 * A factory that uses a pretrained metric.
 * Any calls to train, read, or write the metric are ignored.
 *
 * @author Shilad Sen
 */
public class PretrainedSRFactory implements MonolingualSRFactory {
    private final SRMetric metric;

    public PretrainedSRFactory(SRMetric metric) {
        this.metric = metric;
    }
    @Override
    public SRMetric create() {
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

    public static class PretrainedMetric implements SRMetric {
        private final SRMetric delegate;

        public PretrainedMetric(SRMetric delegate) {
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
        public void write() throws IOException {}
        @Override
        public void read() throws IOException {}
        @Override
        public void trainSimilarity(Dataset dataset) throws DaoException {}
        @Override
        public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {}


    }
}
