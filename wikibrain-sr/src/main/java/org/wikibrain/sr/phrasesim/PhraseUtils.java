package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.sr.vector.SparseVectorSRMetric;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class PhraseUtils {
    public static TIntFloatMap getPhraseVector(SparseVectorSRMetric metric, String phrase) {

        // try using phrase generator directly
        try {
            return metric.getGenerator().getVector(phrase);
        } catch (UnsupportedOperationException e) {
            // try using other methods
        }
        try {
            Language lang = metric.getLanguage();
            LocalId best =  metric.getDisambiguator().disambiguateTop(new LocalString(lang, phrase), null);
            if (best == null) {
                return null;
            }
            return metric.getPageVector(best.getId());
        } catch (DaoException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TLongFloatMap intMap2LongMap(TIntFloatMap map) {
        if (map == null) {
            return null;
        }
        final TLongFloatMap result = new TLongFloatHashMap();
        map.forEachEntry(new TIntFloatProcedure() {
            @Override
            public boolean execute(int k, float v) {
                result.put(k, v);
                return true;
            }
        });
        return result;
    }
}
