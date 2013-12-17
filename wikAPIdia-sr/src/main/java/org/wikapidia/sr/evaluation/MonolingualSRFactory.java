package org.wikapidia.sr.evaluation;

import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.MonolingualSRMetric;

/**
 * @author Shilad Sen
 */
public interface MonolingualSRFactory {
    /**
     * Creates a new, uninitialized SR metric.
     * @return
     */
    MonolingualSRMetric create();

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
