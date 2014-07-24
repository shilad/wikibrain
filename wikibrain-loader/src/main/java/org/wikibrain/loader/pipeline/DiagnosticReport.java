package org.wikibrain.loader.pipeline;

import org.wikibrain.core.lang.LanguageSet;

import java.io.PrintWriter;
import java.util.Map;

import jnt.scimark2.FFT;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

/**
 * @author Shilad Sen
 */
public class DiagnosticReport {
    public void diagnosticReport(PrintWriter writer, LanguageSet langs, Map<String, PipelineStage> stages) {

    }

    public double timeSingleCpu() {
        long before = System.currentTimeMillis();
        microBench();
        long after = System.currentTimeMillis();
        return (after - before) / 1000.0;
    }

    public double timeAllThreads() {
        int numThreads = WpThreadUtils.getMaxThreads();
        long before = System.currentTimeMillis();
        ParallelForEach.range(0, numThreads, new Procedure<Integer>() {
            @Override
            public void call(Integer arg) throws Exception {
                microBench();
            }
        });
        long after = System.currentTimeMillis();
        return (after - before) / 1000.0 / WpThreadUtils.getMaxThreads();
    }

    private void microBench() {
        for (int i = 0; i < 100; i++) {
            FFT.test(FFT.makeRandom(65536));
        }
    }

    public static void main(String args[]) {
        DiagnosticReport report = new DiagnosticReport();
        System.out.println(report.timeSingleCpu());
        System.out.println(report.timeAllThreads());
    }
}
