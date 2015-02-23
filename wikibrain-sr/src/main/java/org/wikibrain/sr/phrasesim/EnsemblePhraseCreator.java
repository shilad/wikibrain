package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.sr.vector.VectorBasedSRMetric;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: make the individual metrics, and the linear combination of them, trainable.
 *
 * @author Shilad Sen
 */
public class EnsemblePhraseCreator implements PhraseCreator {
    private static final Logger LOGGER = Logger.getLogger(EnsemblePhraseCreator.class.getName());

    private final VectorBasedSRMetric[] metrics;
    private double coefficients[];

    public EnsemblePhraseCreator(VectorBasedSRMetric metrics[], double coefficients[]) {
        if (coefficients.length != metrics.length) {
            throw new IllegalArgumentException();
        }
        this.metrics = metrics;
        this.coefficients = coefficients;
    }

    @Override
    public TLongFloatMap getVector(String phrase) {
        final TLongFloatMap vector = new TLongFloatHashMap();
        for (int i = 0; i < metrics.length; i++) {
            try {
                TIntFloatMap v = PhraseUtils.getPhraseVector(metrics[i], phrase);
                if (v == null) continue;
                final int finalI = i;
                v.forEachEntry(new TIntFloatProcedure() {
                    @Override
                    public boolean execute(int key, float value) {
                        long id = ((long) finalI) * Integer.MAX_VALUE + key;
                        vector.put(id, (float) (value * coefficients[finalI]));
                        return true;
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "SR call for " + phrase + ", metric " + metrics[i].getName() + " failed", e);
            }
        }
        return vector;
    }

}
