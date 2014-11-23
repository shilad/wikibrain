package org.wikibrain.loader.pipeline;

/**
* @author Shilad Sen
*/
public class StageFailedException extends Exception {
    private final PipelineStage stage;
    private final int exitCode;


    public StageFailedException(PipelineStage stage, int exitCode) {
        super("Stage " + stage.getName() + " failed with status code " + exitCode);
        this.stage = stage;
        this.exitCode = exitCode;
    }

    public PipelineStage getStage() {
        return stage;
    }

    public int getExitCode() {
        return exitCode;
    }
}
