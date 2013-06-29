package org.wikapidia.sr.normalize;

public class IdentityNormalizer extends BaseNormalizer{
    @Override
    public double normalize(double x) { return x; }

    @Override
    public void observe(double x, double y){}

    @Override
    public void observe(double x) {}

    @Override
    public void observationsFinished() {}

    @Override
    public String dump() {
        return "identity normalizer";
    }

    @Override
    public boolean isTrained() {
        return true;
    }
}
