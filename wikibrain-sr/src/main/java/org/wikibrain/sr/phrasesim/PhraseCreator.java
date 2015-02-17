package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;

/**
 * @author Shilad Sen
 */
public interface PhraseCreator {
    TLongFloatMap getVector(String phrase);
}
