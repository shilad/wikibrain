package org.wikibrain.loader.pipeline;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.apache.commons.io.FileSystemUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.spatial.dao.postgis.PostGISDB;
import org.wikibrain.spatial.loader.SpatialDataLoader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class DiagnosticReport {
    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticReport.class);

    public static double FUDGE_FACTOR = 1.2;    // Better to underpromise than overpromise

    public interface Diagnostic {

        /**
         * Runs a diagnostic and outputs the results using the writer.
         * @param writer
         * @return true if the diagnostic passed.
         */
        public boolean runDiagnostic(PrintWriter writer);
    }

    private final Env env;
    private final Map<String, PipelineStage> stages;
    private final LanguageSet langs;
    private List<? extends Diagnostic> diagnostics;

    public DiagnosticReport(Env env, LanguageSet langs, Map<String, PipelineStage> stages) {
        this.env = env;
        this.langs = langs;
        this.stages = stages;
        this.diagnostics = Arrays.asList(
                new DownloadDiagnostic(),
                new CompletionTimeDiagnostic(),
                new DiskSpaceDiagnostic(),
                new MemoryDiagnostic(),
                new DatabaseDiagnostic(),
                new SpatialDatabaseDiagnostic()
        );
    }

    public boolean runDiagnostics(PrintWriter writer) {
        StringBuilder succeeded = new StringBuilder();
        StringBuilder failed = new StringBuilder();

        for (Diagnostic d : diagnostics) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            boolean passed = d.runDiagnostic(pw);
            if (sw.getBuffer().length() == 0) {
                continue;
            }
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

        return failed.length() == 0;
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
            double allocatedGBs = Runtime.getRuntime().maxMemory() / (1000.0*1000.0*1000.0);
            boolean passed =  (allocatedGBs + 0.01 >= necessaryGBs);
            if (passed) {
                writer.write("Amount of memory allocated for the JVM is okay\n");
            } else {
                writer.write("Not enough memory has been allocated for the JVM!\n");
            }
            writer.write(String.format("\tmemory required: %.1fGB\n", (double)necessaryGBs));
            writer.write(String.format("\tmemory allocated: %.1fGB\n", allocatedGBs));
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
                    double mbs = stage.estimateDiskMegabytes(langs) * FUDGE_FACTOR;;
                    total += mbs;
                    stageMbs.put(stage.getName(), mbs);
                }
            }
            if (getAvailableDiskInMBs() >= total) {
                writer.write(
                        String.format("Disk space is okay. (need %.3f GBs, have %.3f GBs)\n",
                                total / 1024.0, getAvailableDiskInMBs() / 1024.0));
            } else {
                writer.write(
                        String.format("NOT ENOUGH DISK SPACE! (need %.3f GBs, have %.3f GBs)\n",
                                total / 1024.0, getAvailableDiskInMBs() / 1024.0));
            }
            writer.write("\tWarning: Available disk space may be INACCURATE if you have multiple drives.\n");
            for (String name : stageMbs.keySet()) {
                writer.write(String.format("\tstage %s: %.1f MBs\n", name, stageMbs.get(name)));
            }
            return true;
        }
    }

    class DownloadDiagnostic implements Diagnostic {
        @Override
        public boolean runDiagnostic(PrintWriter writer) {
            double total = 0.0;
            LinkedHashMap<String, Double> stageMbs = new LinkedHashMap<String, Double>();
            for (PipelineStage stage : stages.values()) {
                if (stage.hasBeenRun()) {
                    double mbs = stage.estimateDownloadMegabytes(langs) * FUDGE_FACTOR;
                    total += mbs;
                    if (mbs > 0.01) {
                        stageMbs.put(stage.getName(), mbs);
                    }
                }
            }
            writer.write(String.format("Rough estimate of download size: %.1f MBs\n",  total));
            writer.write("\tThis may be an over-estimate if some files have already been downloaded.\n");
            writer.write(String.format("\tTime on dial-up (50kbs): %.1f minutes\n", total / 0.005 / 60));
            writer.write(String.format("\tTime on Broadband (1Mbs): %.1f minutes\n", total / 0.1 / 60));
            writer.write(String.format("\tTime on Broadband (10Mbs): %.1f minutes\n", total / 1 / 60));
            writer.write(String.format("\tTime on Broadband (100Mbs): %.1f minutes\n", total / 10 / 60));
            for (String name : stageMbs.keySet()) {
                writer.write(String.format("\tstage %s will download about %.1f about MBs\n", name, stageMbs.get(name)));
            }
            return true;
        }
    }

    class DatabaseDiagnostic implements Diagnostic {
        @Override
        public boolean runDiagnostic(PrintWriter writer) {
            Config config = null;
            try {
                config = env.getConfigurator().getConfig(WpDataSource.class, null);
            } catch (ConfigurationException e) {
                throw new IllegalStateException(e);
            }

            boolean passed = true;
            try {
                WpDataSource ds = env.getConfigurator().get(WpDataSource.class);
                ds.getConnection().close();
                writer.write("Connection to database succeeded. Active configuration:\n");
            } catch (Exception e) {
                writer.write("Connection to database FAILED! Active configuration:\n");
                passed = false;
            }
            for (Map.Entry<String, ConfigValue > entry : config.entrySet()) {
                writer.write("\t" + entry.getKey() + ": " + entry.getValue().render() + "\n");
            }
            return passed;
        }
    }


    class SpatialDatabaseDiagnostic implements Diagnostic {
        @Override
        public boolean runDiagnostic(PrintWriter writer) {
            boolean found = false;
            for (PipelineStage stage : stages.values()) {
                if (stage.getKlass() == SpatialDataLoader.class && stage.hasBeenRun()) {
                    found = true;
                }
            }
            if (!found) {
                return true;
            }
            Config config = null;
            try {
                config = env.getConfigurator().getConfig(PostGISDB.class, null);
            } catch (ConfigurationException e) {
                throw new IllegalStateException(e);
            }

            boolean passed = true;
            try {
                PostGISDB ds = env.getConfigurator().get(PostGISDB.class);
                writer.write("Connection to spatial database succeeded. Active configuration:\n");
            } catch (Exception e) {
                writer.write("Connection to spatial database FAILED! Active configuration:\n");
                passed = false;
            }
            for (Map.Entry<String, ConfigValue > entry : config.entrySet()) {
                writer.write("\t" + entry.getKey() + ": " + entry.getValue().render() + "\n");
            }
            return passed;
        }
    }

    private double getAvailableDiskInMBs() {
        try {
            return FileSystemUtils.freeSpaceKb(10000) / 1024;
        } catch (IOException e) {
            LOG.warn("failed to calculate free space in current dir:", e);
            return 0.0;
        }
    }
}
