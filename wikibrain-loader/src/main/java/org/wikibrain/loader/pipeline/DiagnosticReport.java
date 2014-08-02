package org.wikibrain.loader.pipeline;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.utils.JvmUtils;
import org.wikibrain.utils.WbCommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class DiagnosticReport {
    public static double FUDGE_FACTOR = 1.2;    // Better to underpromise than overpromise

    public interface Diagnostic {

        /**
         * Runs a diagnostic and outputs the results using the writer.
         * @param writer
         * @return true if the diagnostic passed.
         */
        public boolean runDiagnostic(PrintWriter writer);
    }

    private final Map<String, PipelineStage> stages;
    private final LanguageSet langs;
    private List<? extends Diagnostic> diagnostics;

    public DiagnosticReport(LanguageSet langs, Map<String, PipelineStage>stages) {
        this.langs = langs;
        this.stages = stages;
        this.diagnostics = Arrays.asList(
                new CompletionTimeDiagnostic(),
                new DiskSpaceDiagnostic(),
                new MemoryDiagnostic()
        );
    }

    public boolean runDiagnostics(PrintWriter writer) {
        StringBuilder succeeded = new StringBuilder();
        StringBuilder failed = new StringBuilder();

        for (Diagnostic d : diagnostics) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            boolean passed = d.runDiagnostic(pw);
            pw.append("\n");
            pw.close();
            if (passed) {
                succeeded.append(sw.getBuffer());
            } else {
                failed.append(sw.getBuffer());
            }
        }
        if (failed.length() > 0) {
            writer.write("***********************************\n");
            writer.write("** SOME DIAGNOSTIC TESTS FAILED! **\n");
            writer.write("***********************************\n\n");
            writer.write("DIAGNOSTICS THAT FAILED:\n");
            writer.write("=======================\n\n");
            writer.write(failed.toString());
            writer.write("\n\nDIAGNOSTICS THAT SUCCEEDED:\n");
            writer.write("===========================\n\n");
        } else {
            writer.write("*************************************\n");
            writer.write("** ALL DIAGNOSTIC TESTS SUCCEEDED! **\n");
            writer.write("*************************************\n\n");
        }

        writer.write(succeeded.toString());

        return false;
//        return failed.length() > 0;
    }

    class MemoryDiagnostic implements Diagnostic {

        @Override
        public boolean runDiagnostic(PrintWriter writer) {
            int articles = 0;
            for (Language lang : langs) {
                articles += LanguageInfo.getByLanguage(lang).getNumArticles();
            }
            int necessaryGBs;
            if (articles < 100000) {
                necessaryGBs = 2;
            } else if (articles < 200000) {
                necessaryGBs = 3;
            } else if (articles < 400000) {
                necessaryGBs = 4;
            } else if (articles < 1600000) {
                necessaryGBs = 6;
            } else {
                necessaryGBs = 8;
            }

            // TODO: Should we use 1024^3 to be more accurate?
            double allocatedGBs = Runtime.getRuntime().maxMemory() / (1000*1000*1000);
            boolean passed =  (allocatedGBs + 0.01 >= necessaryGBs);
            if (passed) {
                writer.write("Amount of memory allocated for the JVM is okay\n");
            } else {
                writer.write("Not enough memory has been allocated for the JVM!\n");
            }
            writer.write("\tmemory required, in GBs: " + necessaryGBs + "GB\n");
            writer.write("\tmemory allocated, in GBs: " + allocatedGBs + "GB\n");
            return passed;
        }
    }

    class CompletionTimeDiagnostic implements Diagnostic {
        @Override
        public boolean runDiagnostic(PrintWriter writer) {
            double total = 0.0;
            LinkedHashMap<String, Double> stageMinutes = new LinkedHashMap<String, Double>();
            for (PipelineStage stage : stages.values()) {
                if (stage.hasBeenRun()) {
                    double minutes = stage.estimateSeconds(langs) / 60.0 * FUDGE_FACTOR;
                    stageMinutes.put(stage.getName(), minutes);
                    total += minutes;
                }
            }
            writer.write(String.format(
                    "Completion time estimate: %.1f minutes (NOT including download time)\n",
                    total));
            for (String name : stageMinutes.keySet()) {
                writer.write(String.format("\tstage %s: %.1f minutes\n", name, stageMinutes.get(name)));
            }
            return true;
        }
    }

    class DiskSpaceDiagnostic implements Diagnostic {
        @Override
        public boolean runDiagnostic(PrintWriter writer) {
            double total = 0.0;
            LinkedHashMap<String, Double> stageMbs = new LinkedHashMap<String, Double>();
            for (PipelineStage stage : stages.values()) {
                if (stage.hasBeenRun()) {
                    double mbs = stage.estimateMegabytes(langs) * FUDGE_FACTOR;;
                    total += mbs;
                    stageMbs.put(stage.getName(), mbs);
                }
            }
            if (getAvailableDiskInMBs() >= total) {
                writer.write(
                        String.format("Disk space is okay. (need %.3f GBs, have %.3f)\n",
                                total / 1024.0, getAvailableDiskInMBs()));
            } else {
                writer.write(
                        String.format("NOT ENOUGH DISK SPACE! (need %.3f GBs, have %.3f)\n",
                                total / 1024.0, getAvailableDiskInMBs()));
            }
            for (String name : stageMbs.keySet()) {
                writer.write(String.format("\tstage %s: %.1f MBs\n", name, stageMbs.get(name)));
            }
            return getAvailableDiskInMBs() >= total;
        }
    }

    class DatabaseDiagnostic implements Diagnostic {
        @Override
        public boolean runDiagnostic(PrintWriter writer) {
            return true;
        }
    }



    private double getAvailableDiskInMBs() {
        long bytes = 0;
        for (File file : File.listRoots()) {
            bytes += file.getFreeSpace();
        }
        return bytes / (1024.0 * 1024.0);
    }
}
