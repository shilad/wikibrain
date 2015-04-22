package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TLongFloatMap;
import org.wikibrain.sr.vector.SparseVectorSRMetric;

/**
 * @author Shilad Sen
 */
public class SimplePhraseCreator implements PhraseCreator {
    SparseVectorSRMetric metric;

    public SimplePhraseCreator(SparseVectorSRMetric metric) {
        this.metric = metric;
    }

    @Override
    public TLongFloatMap getVector(String phrase) {
        return PhraseUtils.intMap2LongMap(
                PhraseUtils.getPhraseVector(metric, phrase));
    }
}
