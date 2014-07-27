package org.wikibrain.loader.pipeline;

import jnt.scimark2.MonteCarlo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.io.PrintWriter;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class DiagnosticReport {

    /**
     * A heuristic estimate of single CPU speed.
     * A 2012 Macbook Retina Pro with a 2.6 GHz i7 is about 1.0
     */
    private static double singleCoreSpeed = -1;


    /**
     * A heuristic estimate of multi CPU speed.
     * A 2012 Macbook Retina Pro with a 2.6 GHz i7 is about 6.0
     */
    private static double multiCoreSpeed = -1;


    public void diagnosticReport(PrintWriter writer, LanguageSet langs, Map<String, PipelineStage> stages) {

    }

    public double getSingleCoreSpeed() {
        if (singleCoreSpeed < 0) {
            benchmark();
        }
        return singleCoreSpeed;
    }

    public double getMultiCoreSpeed() {
        if (multiCoreSpeed < 0) {
            benchmark();
        }
        return multiCoreSpeed;
    }

    public synchronized void benchmark() {
        long before = System.currentTimeMillis();
        microBench();
        long after = System.currentTimeMillis();
        double secs = (after - before) / 1000.0;
        singleCoreSpeed = 0.5 / secs;

        int numThreads = WpThreadUtils.getMaxThreads();
        before = System.currentTimeMillis();
        ParallelForEach.range(0, numThreads, new Procedure<Integer>() {
            @Override
            public void call(Integer arg) throws Exception {
                microBench();
            }
        });
        after = System.currentTimeMillis();
        secs = (after - before) / 1000.0 / WpThreadUtils.getMaxThreads();
        multiCoreSpeed = 0.5 / secs;
    }

    private void microBench() {
        for (int i = 0; i < 100; i++) {
            MonteCarlo.integrate(100000);
        }
    }

    public static void main(String args[]) {
        DiagnosticReport report = new DiagnosticReport();
        System.out.println(report.getSingleCoreSpeed());
        System.out.println(report.getMultiCoreSpeed());
    }
}
