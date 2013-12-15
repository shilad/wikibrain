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

    /**
     * Returns a string describing the disambig config. Used in the results output.
     */
    String describeDisambiguator();

    /**
     * Returns a string describing the sr config. Used in the results output.
     */
    String describeMetric();

    /**
     * Returns the name of the generated metric.
     * @return
     */
    String getName();
}
