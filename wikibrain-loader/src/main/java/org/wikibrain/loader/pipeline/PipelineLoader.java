package org.wikibrain.loader.pipeline;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.MetaInfo;

import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Runs stages in the pipeline.
 * The stages are specified in the reference.conf and can be turned on or off using command line params.
 *
 * @author Shilad Sen
 */
public class PipelineLoader {
    public static final String DEFAULT_GROUP = "core";
    public static final String MULTILINGUAL_GROUP = "multilingual-core";

    public static Logger LOG = LoggerFactory.getLogger(PipelineLoader.class);

    private final Map<String, MetaInfo> state;
    private final LanguageSet langs;
    private final LinkedHashMap<String, PipelineStage> stages = new LinkedHashMap<String, PipelineStage>();
    private final Map<String, List<String>> groups = new HashMap<String, List<String>>();
    private final DiagnosticDao diagnosticDao;
    private boolean forceRerun = false;

    public PipelineLoader(Env env) throws ConfigurationException, DaoException, ClassNotFoundException, InterruptedException {
        this(env, null);
    }

    public PipelineLoader(Env env, List<StageArgs> args) throws ConfigurationException, DaoException, ClassNotFoundException, InterruptedException {
        MetaInfoDao metaDao = env.getConfigurator().get(MetaInfoDao.class);
        this.langs = env.getLanguages();
        this.state = metaDao.getAllCummulativeInfo();
        this.diagnosticDao = env.getConfigurator().get(DiagnosticDao.class);

        initConfig(env.getConfiguration());
        if (args == null) {
            if (langs.size() == 0) {
                throw new IllegalArgumentException("No languages specified to pipeline loader");
            } else if (langs.size() == 1) {
                args = Arrays.asList(new StageArgs(DEFAULT_GROUP, true, null));
            } else {
                args = Arrays.asList(new StageArgs(MULTILINGUAL_GROUP, true, null));
            }
        }
        setStageArguments(args);
    }

    public boolean runDiagnostics(Env env, String[] args, PrintStream writer) throws IOException, InterruptedException {
        PrintWriter pw = new PrintWriter(writer);
        boolean b = runDiagnostics(env, args, pw);
        pw.flush();
        return b;
    }

    public synchronized boolean runDiagnostics(Env env, String[] args, PrintWriter writer) throws IOException, InterruptedException {
        for (PipelineStage stage : stages.values()) {
            stage.reset();
            stage.setDryRun(true);
        }

        LOG.info("Beginning dry run");
        for (PipelineStage stage : stages.values()) {
            if (stage.getShouldRun() != null && stage.getShouldRun()) {
                try {
                    stage.runWithDependenciesIfNeeded(args, forceRerun);
                } catch (StageFailedException e) {
                    throw new IllegalStateException(e); // shouldn't happen
                }
            }
        }

        DiagnosticReport report = new DiagnosticReport(env, langs, stages);
        boolean result = report.runDiagnostics(writer);

        LOG.info("Ended dry run");
        for (PipelineStage stage : stages.values()) {
            stage.reset();
        }

        return result;
    }


    public synchronized void run(String [] args) throws IOException, InterruptedException, StageFailedException {
        for (PipelineStage stage : stages.values()) {
            stage.reset();
        }
        LOG.info("Beginning loading");
        try {
            for (PipelineStage stage : stages.values()) {
                if (stage.getShouldRun() != null && stage.getShouldRun()) {
                    LOG.info("Beginning stage " + stage.getName());
                    stage.runWithDependenciesIfNeeded(args, forceRerun);
                    LOG.info("Successfully completed stage " + stage.getName());
                }
            }
            LOG.info("Loading successfully finished");
        } finally {
            quietlySaveDiagnostics();
        }
    }

    private void quietlySaveDiagnostics() {
        try {
            long runId = Math.abs(new Random().nextLong());
            for (PipelineStage stage : stages.values()) {
                if (stage != null && stage.hasBeenRun()) {
                    StageDiagnostic sd = new StageDiagnostic(
                            runId,
                            stage.getName(),
                            langs,
                            stage.getElapsedSeconds(),
                            CpuBenchmarker.getSingleCoreSpeed(),
                            CpuBenchmarker.getMultiCoreSpeed(),
                            -1.0
                    );
                    sd.setSucceeded(stage.getSucceeded());
                    diagnosticDao.saveQuietly(sd);
                }
            }
        } catch (Exception e) {
            LOG.warn("Diagnostics save failed (this should be harmless):", e);
        }
    }

    private void initConfig(Configuration config) throws ClassNotFoundException {
        for (Config stageConfig : config.get().getConfigList("loader.stages")) {
            PipelineStage stage = new PipelineStage(stageConfig, stages.values(), state);
            stages.put(stage.getName(), stage);
        }

        // Set up the groups
        Config groupConfig = config.get().getConfig("loader.groups");
        for (String g : config.get().getObject("loader.groups").keySet()) {
            groups.put(g, new ArrayList<String>());
            for (String s : groupConfig.getStringList(g)) {
                PipelineStage stage = getStage(s);  // throws IllegalArgumentException if unknown stage
                groups.get(g).add(s);
            }
        }
    }

    private void setStageArguments(List<StageArgs> stageArgs) {
        // expand groups in the options to the individual stages
        List<StageArgs> expandedArgs = new ArrayList<StageArgs>();
        for (StageArgs sa : stageArgs) {
            if (groups.containsKey(sa.getStageName())) {
                for (String s : groups.get(sa.getStageName())) {
                    expandedArgs.add(sa.copyWithName(s));
                }
            } else {
                expandedArgs.add(sa);
            }
        }

        // Run with the requested options
        for (StageArgs sa : expandedArgs) {
            PipelineStage stage = getStage(sa.getStageName());
            stage.setOverrideOptions(sa.isShouldRun(), sa.getArgs());
        }
    }

    private PipelineStage getStage(String name) {
        PipelineStage stage = stages.get(name);
        if (stage == null) {
            throw new IllegalArgumentException("Unknown stage: " + name);
        }
        return stage;
    }


    public void setForceRerun(boolean forceRerun) {
        this.forceRerun = forceRerun;
    }
}
