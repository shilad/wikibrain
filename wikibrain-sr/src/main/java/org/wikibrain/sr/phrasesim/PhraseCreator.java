package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;
import org.wikibrain.sr.dataset.Dataset;

import java.util.List;

/**
 * @author Shilad Sen
 */
public interface PhraseCreator {
    public TLongFloatMap getVector(String phrase);
}
