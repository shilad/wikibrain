package org.wikibrain.loader.pipeline;

import org.wikibrain.utils.WbCommandLine;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *
 * Represents user-specified stage arguments that
 * determine which stages to run and override configuration defaults.
 * @author Shilad Sen
 */
public class StageArgs {
    private final String stageName;
    private boolean shouldRun = true;
    private String [] args = null;

    public StageArgs(String opts) {
        String tokens[] =  opts.split(":", 3);  // "stage:{on|off}:args
        stageName = tokens[0];

        if (tokens.length >= 2) {
            if (!Arrays.asList("on", "off").contains(tokens[1])) {
                throw new IllegalArgumentException("arg format for -s is stagename:{on|off}[:args]");
            }
            shouldRun = tokens[1].equals("on");
        }

        if (tokens.length == 3) {
            args = WbCommandLine.translateCommandline(tokens[2]);
        }
    }

    public StageArgs(String stageName, boolean shouldRun, String[] args) {
        this.stageName = stageName;
        this.shouldRun = shouldRun;
        this.args = args;
    }

    public StageArgs copyWithName(String newStageName) {
        return new StageArgs(newStageName, shouldRun, args);
    }

    public String getStageName() {
        return stageName;
    }

    public boolean isShouldRun() {
        return shouldRun;
    }

    public String[] getArgs() {
        return args;
    }
}
