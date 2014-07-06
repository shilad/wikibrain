package org.wikibrain.dao.load;

import com.typesafe.config.Config;
import org.apache.commons.lang3.ArrayUtils;
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
     * Whether or not the stage has already been run this Pipeline execution.
     */
    private boolean hasBeenRun = false;

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

    public void runWithDependenciesIfNeeded(String [] cmdLineArgs, boolean forceRerun) throws IOException, InterruptedException {
        for (PipelineStage stage : dependsOn) {
            stage.runWithDependenciesIfNeeded(cmdLineArgs, forceRerun);
        }
        if (isNeeded(forceRerun)) {
            run(cmdLineArgs);
        }
    }

    public void run(String [] cmdLineArgs) throws IOException, InterruptedException {
        String [] args;
        if (argsOverride == null) {
            args = ArrayUtils.addAll(cmdLineArgs, extraArgs);
        } else {
            args = argsOverride;
        }
        Process p = JvmUtils.launch(klass, args);
        int retVal = p.waitFor();
        if (retVal != 0) {
            System.err.println("command failed with exit code " + retVal + " : ");
            System.err.println("ABORTING!");
            System.exit(retVal);
        }
        hasBeenRun = true;
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



    private PipelineStage getStage(Collection<PipelineStage> previousStages, String stage) {
        for (PipelineStage s : previousStages) {
            if (s.name.equalsIgnoreCase(stage)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown pipeline stage: " + stage);
    }
}
