package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.sr.vector.VectorBasedSRMetric;

import java.io.IOException;
import java.util.List;

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
        return PhraseUtils.intMap2FloatMap(
                PhraseUtils.getPhraseVector(metric, phrase));
    }
}
