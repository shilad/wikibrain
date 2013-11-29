package org.wikapidia.sr.evaluation;

import org.wikapidia.sr.LocalSRMetric;

/**
 * @author Shilad Sen
 */
public interface LocalSRFactory {
    /**
     * Creates a new, uninitialized SR metric.
     * @return
     */
    LocalSRMetric create();
}
