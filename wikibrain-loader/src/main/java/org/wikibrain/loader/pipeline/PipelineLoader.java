package org.wikibrain.loader.pipeline;

import com.typesafe.config.Config;
import org.apache.commons.cli.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.MetaInfo;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static Logger LOG = java.util.logging.Logger.getLogger(PipelineLoader.class.getName());

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

    public void dryRun(String [] args) throws IOException, InterruptedException {
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
        LOG.info("Ended dry run");
        for (PipelineStage stage : stages.values()) {
            stage.reset();
            stage.setDryRun(true);
        }
    }

    public void run(String [] args) throws IOException, InterruptedException, StageFailedException {
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
            DiagnosticReport report = new DiagnosticReport();
            for (PipelineStage stage : stages.values()) {
                if (stage != null && stage.hasBeenRun()) {
                    StageDiagnostic sd = new StageDiagnostic(
                            stage.getName(),
                            langs,
                            stage.getElapsedSeconds(),
                            report.getSingleCoreSpeed(),
                            report.getMultiCoreSpeed(),
                            -1.0
                    );
                    sd.setSucceeded(stage.getSucceeded());
                    diagnosticDao.saveQuietly(sd);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Diagnostics save failed (this should be harmless):", e);
        }
    }

    public void initConfig(Configuration config) throws ClassNotFoundException {
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

    public void setStageArguments(List<StageArgs> stageArgs) {
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
