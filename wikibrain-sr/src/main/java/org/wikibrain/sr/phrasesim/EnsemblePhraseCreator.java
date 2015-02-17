package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.sr.vector.VectorBasedSRMetric;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class EnsemblePhraseCreator implements PhraseCreator {
    private final VectorBasedSRMetric[] metrics;
    private double coefficients[];

    public EnsemblePhraseCreator(VectorBasedSRMetric metrics[], double coefficients[]) {
        if (coefficients.length != metrics.length + 1) {
            throw new IllegalArgumentException();
        }
        this.metrics = metrics;
        this.coefficients = coefficients;
    }

    @Override
    public TLongFloatMap getVector(String phrase) {
        return null;
    }
}
