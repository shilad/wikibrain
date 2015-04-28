package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;
import org.wikibrain.sr.vector.SparseVectorSRMetric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: make the individual metrics, and the linear combination of them, trainable.
 *
 * @author Shilad Sen
 */
public class EnsemblePhraseCreator implements PhraseCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnsemblePhraseCreator.class);

    private final SparseVectorSRMetric[] metrics;
    private double coefficients[];

    public EnsemblePhraseCreator(SparseVectorSRMetric metrics[], double coefficients[]) {
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
                LOGGER.info("SR call for " + phrase + ", metric " + metrics[i].getName() + " failed", e);
            }
        }
        return vector;
    }

}
