package org.wikibrain.loader.pipeline;

import com.typesafe.config.Config;
import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.MetaInfo;
import org.wikibrain.utils.JvmUtils;

import java.io.IOException;
import java.util.*;

/**
* @author Shilad Sen
*/
public class PipelineStage {
    /**
     * Name of the stage
     */
    private final String name;

    /**
     * The class whose main method should be run for this stage
     */
    private final Class klass;

    /**
     * Stages required to be run before this stage.
     */
    private List<PipelineStage> dependsOn = new ArrayList<PipelineStage>();

    /**
     * (One of) the class that is loaded during this stage.
     */
    private final String loadsClass;

    /**
     * Stage-specific args that should be appended to any standard args.
     */
    private final String extraArgs[];

    // Explicit user request, if it exists
    private Boolean shouldRun;

    // Explicit arguments requested by user;  takes precidence over extraArgs.
    private String [] argsOverride;

    /**
     * Information about what was loaded for this stage at the beginning of Pipeline execution.
     */
    private MetaInfo loadedInfo;

    /**
     * If true, don't actually run things. Just record what you would have run.
     */
    private boolean dryRun = false;

    /**
     * Arguments used during the previous run.
     * Null indicates that the stage was not run.
     */
    private String actualArgs[] = null;

    /**
     * Whether or not the stage has already been run this Pipeline execution.
     */
    private boolean hasBeenRun = false;

    /**
     * Time the stage started.
     */
    private Date startTime = null;

    /**
     * Time the stage required.
     */
    private double elapsedSeconds = 0;

    /**
     * Whether the stage succeded or failed
     */
    private Boolean succeeded = null;

    /**
     * Equation used to estimate the time required for a particular stage.
     */
    private final String timeEstimateEquation;

    /**
     * Equation used to estimate the disk space required for a particular stage in MBs.
     */
    private final String diskEstimateEquation;

    /**
     * Equation used to estimate the disk space required for a particular stage in MBs.
     */
    private final String downloadEstimateEquation;

    public PipelineStage(Config config, Collection<PipelineStage> previousStages, Map<String, MetaInfo> loadedInfo) throws ClassNotFoundException {
        this.name = config.getString("name");
        this.klass = Class.forName(config.getString("class"));
        this.extraArgs = config.getStringList("extraArgs").toArray(new String[0]);
        this.loadsClass =  config.hasPath("loadsClass") ? config.getString("loadsClass") : null;
        if (config.hasPath("dependsOnStage")) {
            Object obj = config.getAnyRef("dependsOnStage");
            if (obj instanceof String) {
                dependsOn.add(getStage(previousStages, (String)obj));
            } else if (obj instanceof List) {
                for (String s : (List<String>)obj) {
                    dependsOn.add(getStage(previousStages, s));
                }
            } else {
                throw new IllegalArgumentException("Invalid dependsOn value for pipeline stage " + name + ": " + obj);
            }
        }
        this.timeEstimateEquation = config.getString("runtime");
        this.diskEstimateEquation = config.getString("diskSpace");
        if (config.hasPath("downloadSize")) {
            this.downloadEstimateEquation = config.getString("downloadSize");
        } else {
            this.downloadEstimateEquation = "0.0";
        }
        this.loadedInfo = loadsClass == null ? null : loadedInfo.get(loadsClass);
    }

    public void setOverrideOptions(Boolean run, String args[]) {
        this.shouldRun = run;
        this.argsOverride = args;
    }

    public boolean isNeeded(boolean forceRerun) {
        if (hasBeenRun()) {                             // if run this execution cycle, skip
            return false;
        } else if (shouldRun != null && !shouldRun) {   // if user said not to run, skip
            return false;
        } else if (forceRerun) {                        // if we should rerun everything, rerun
            return true;
        } else {                                        // check to see if the class is loaded
            return loadedInfo == null || loadedInfo.getNumRecords() == 0;
        }
    }

    public void runWithDependenciesIfNeeded(String [] cmdLineArgs, boolean forceRerun) throws IOException, InterruptedException, StageFailedException {
        for (PipelineStage stage : dependsOn) {
            stage.runWithDependenciesIfNeeded(cmdLineArgs, forceRerun);
        }
        if (isNeeded(forceRerun)) {
            run(cmdLineArgs);
        }
    }

    public void run(String [] cmdLineArgs) throws IOException, InterruptedException, StageFailedException {
        if (argsOverride == null) {
            actualArgs = ArrayUtils.addAll(cmdLineArgs, extraArgs);
        } else {
            actualArgs = ArrayUtils.addAll(cmdLineArgs, argsOverride);
        }

        if (!dryRun) {
            startTime = new Date();
            long before = System.currentTimeMillis();
            Process p = JvmUtils.launch(klass, actualArgs);
            int retVal = p.waitFor();
            if (retVal != 0) {
                hasBeenRun = true;
                succeeded = false;
                throw new StageFailedException(this, retVal);
            }
            succeeded = true;
            long after = System.currentTimeMillis();
            elapsedSeconds = (after - before) / 1000.0;
        }
        hasBeenRun = true;
    }

    public void setDryRun(boolean dryRun) {
        reset();
        this.dryRun = dryRun;
    }

    public String getName() {
        return name;
    }

    public Class getKlass() {
        return klass;
    }

    public boolean hasBeenRun() {
        return hasBeenRun;
    }

    public Boolean getShouldRun() {
        return shouldRun;
    }

    @Override
    public String toString() {
        String deps = new String();
        for (PipelineStage s : dependsOn) {
            if (deps.length() > 0) {
                deps += ", ";
            }
            deps += s;
        }
        return "PipelineStage{" +
                "name='" + name + '\'' +
                ", klass=" + klass +
                ", dependsOn=" + deps +
                ", loadsClass='" + loadsClass + '\'' +
                ", extraArgs=" + Arrays.toString(extraArgs) +
                ", shouldRun=" + shouldRun +
                ", argsOverride=" + Arrays.toString(argsOverride) +
                ", loadedInfo=" + loadedInfo +
                ", hasBeenRun=" + hasBeenRun +
                '}';
    }

    public void reset() {
        dryRun = false;
        hasBeenRun = false;
        argsOverride = null;
    }

    public String[] getActualArgs() {
        return actualArgs;
    }

    public Date getStartTime() {
        return startTime;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public Boolean getSucceeded() {
        return succeeded;
    }

    public double estimateSeconds(LanguageSet langs) {
        int numArticles = 0;
        int numLinks = 0;
        for (Language lang : langs) {
            LanguageInfo li = LanguageInfo.getByLanguage(lang);
            numLinks += li.getNumLinks();
            numArticles += li.getNumArticles();
        }
        Evaluator mathEvaluator = new Evaluator();
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("singleCoreSpeed", ""+CpuBenchmarker.getSingleCoreSpeed());
        variables.put("multiCoreSpeed", ""+CpuBenchmarker.getMultiCoreSpeed());
        variables.put("links", ""+numLinks);
        variables.put("articles", ""+numArticles);
        mathEvaluator.setVariables(variables);
        try {
            return mathEvaluator.getNumberResult(timeEstimateEquation);
        } catch (EvaluationException e) {
            throw new RuntimeException(e);
        }
    }


    public double estimateDiskMegabytes(LanguageSet langs) {
        int numArticles = 0;
        int numLinks = 0;
        for (Language lang : langs) {
            LanguageInfo li = LanguageInfo.getByLanguage(lang);
            numLinks += li.getNumLinks();
            numArticles += li.getNumArticles();
        }
        Evaluator mathEvaluator = new Evaluator();
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("links", ""+numLinks);
        variables.put("articles", ""+numArticles);
        mathEvaluator.setVariables(variables);
        try {
            return mathEvaluator.getNumberResult(diskEstimateEquation);
        } catch (EvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    public double estimateDownloadMegabytes(LanguageSet langs) {
        int numArticles = 0;
        int numLinks = 0;
        for (Language lang : langs) {
            LanguageInfo li = LanguageInfo.getByLanguage(lang);
            numLinks += li.getNumLinks();
            numArticles += li.getNumArticles();
        }
        Evaluator mathEvaluator = new Evaluator();
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("links", ""+numLinks);
        variables.put("articles", ""+numArticles);
        mathEvaluator.setVariables(variables);
        try {
            return mathEvaluator.getNumberResult(downloadEstimateEquation);
        } catch (EvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    private PipelineStage getStage(Collection<PipelineStage> previousStages, String stage) {
        for (PipelineStage s : previousStages) {
            if (s.name.equalsIgnoreCase(stage)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown pipeline stage: " + stage);
    }
}
