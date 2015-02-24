package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TLongFloatMap;
import org.wikibrain.sr.vector.VectorBasedSRMetric;

/**
 * @author Shilad Sen
 */
public class SimplePhraseCreator implements PhraseCreator {
    VectorBasedSRMetric metric;

    public SimplePhraseCreator(VectorBasedSRMetric metric) {
        this.metric = metric;
    }

    @Override
    public TLongFloatMap getVector(String phrase) {
        return PhraseUtils.intMap2LongMap(
                PhraseUtils.getPhraseVector(metric, phrase));
    }
}
