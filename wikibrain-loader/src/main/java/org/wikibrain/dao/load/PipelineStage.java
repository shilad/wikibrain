package org.wikibrain.dao.load;

import com.typesafe.config.Config;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.core.model.MetaInfo;
import org.wikibrain.utils.JvmUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
* @author Shilad Sen
*/
public class PipelineStage {
    /**
     * Name of the stage
     */
    private final String name;

    /**
     * Group this stage belongs to (e.g. "core", "spatial", etc.)
     */
    private final String group;

    /**
     * The class whose main method should be run for this stage
     */
    private final Class klass;

    /**
     * Stages required to be run before this stage.
     * TODO: make this a list.
     */
    PipelineStage dependsOn = null;

    /**
     * (One of) the class that is loaded during this stage.
     */
    private final String loadsClass;

    /**
     * Stage-specific args that should be appended to any standard args.
     */
    private final String extraArgs[];

    // User overrides; takes precidence over extraArgs, etc.
    private Boolean runOverride;
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
        this.group = config.getString("group");
        this.extraArgs = config.getStringList("extraArgs").toArray(new String[0]);
        this.loadsClass =  config.hasPath("loadsClass") ? config.getString("loadsClass") : null;
        if (config.hasPath("dependsOnStage")) {
            String n = config.getString("dependsOnStage");
            for (PipelineStage s : previousStages) {
                if (s.name.equalsIgnoreCase(n)) {
                    dependsOn = s;
                    break;
                }
            }
            if (dependsOn == null) {
                throw new IllegalArgumentException("No stage found with name '" + n + "'");
            }
        }
        this.loadedInfo = loadsClass == null ? null : loadedInfo.get(loadsClass);
    }

    public void setOverrideOptions(Boolean run, String args[]) {
        this.runOverride = run;
        this.argsOverride = args;
    }

    public boolean isNeeded() {
        if (hasBeenRun()) {
            return false;
        } else if (runOverride != null) {
            return runOverride;
        } else {
            return loadedInfo == null || loadedInfo.getNumRecords() == 0;
        }
    }

    public boolean isNeededAtTopLevel(List<String> groups, boolean forceRerun) {
        if (hasBeenRun()) {
            return false;
        } else if (runOverride != null) {
            return runOverride;
        } else if (groups.contains(group)) {
            return forceRerun || loadedInfo == null || loadedInfo.getNumRecords() == 0;
        } else {
            return false;
        }
    }

    public void runWithDependencies(String [] cmdLineArgs) throws IOException, InterruptedException {
        if (hasBeenRun) {
            return;
        }
        if (dependsOn != null && dependsOn.isNeeded()) {
            dependsOn.runWithDependencies(cmdLineArgs);
        }
        run(cmdLineArgs);
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

    public String getGroup() {
        return group;
    }

    public Class getKlass() {
        return klass;
    }

    public boolean hasBeenRun() {
        return hasBeenRun;
    }

    @Override
    public String toString() {
        return "PipelineStage{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", klass=" + klass +
                ", dependsOn=" + ((dependsOn == null) ? "null" : dependsOn.getName()) +
                ", loadsClass='" + loadsClass + '\'' +
                ", extraArgs=" + Arrays.toString(extraArgs) +
                ", runOverride=" + runOverride +
                ", argsOverride=" + Arrays.toString(argsOverride) +
                ", loadedInfo=" + loadedInfo +
                ", hasBeenRun=" + hasBeenRun +
                '}';
    }
}
