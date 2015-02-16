package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
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
public class SimplePhraseCreator {
    VectorBasedSRMetric metric;

    public SimplePhraseCreator(VectorBasedSRMetric metric) {
        this.metric = metric;
    }

    public TIntFloatMap getVector(String phrase) {
        // try using phrase generator directly
        try {
            return metric.getGenerator().getVector(phrase);
        } catch (UnsupportedOperationException e) {
            // try using other methods
        }
        try {
            LocalId best =  metric.getDisambiguator().disambiguateTop(new LocalString(Language.EN, phrase), null);
            return metric.getPageVector(best.getId());
        } catch (DaoException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
