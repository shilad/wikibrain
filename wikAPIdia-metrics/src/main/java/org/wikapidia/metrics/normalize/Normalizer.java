package org.wikapidia.metrics.normalize;

import org.wikapidia.metrics.SRResultList;

import java.io.Serializable;

public interface Normalizer extends Serializable {
    public SRResultList normalize(SRResultList list);
    public double normalize(double x);

    public void observe(SRResultList sims, int rank, double y);
    public void observe(double x, double y);
    public void observe(double x);
    public void observationsFinished();
    public String dump();
    public boolean isTrained();
}
