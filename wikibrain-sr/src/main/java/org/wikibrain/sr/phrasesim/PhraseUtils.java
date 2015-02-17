package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;

/**
 * @author Shilad Sen
 */
public class PhraseUtils {
    public static TLongFloatMap intMap2FloatMap(TIntFloatMap map) {
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
