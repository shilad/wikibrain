package org.wikibrain.loader.pipeline;

import org.wikibrain.core.lang.LanguageSet;

import java.util.Date;

/**
 * @author Shilad Sen
 */
public class StageDiagnostic {

    /**
     * Random, but unique id for each run of the importer.
     * Can be used to track total import time.
     */
    private final long runId;

    /**
     * Name of the stage
     */
    private String stage;

    /**
     * Languages in use
     */
    private LanguageSet langs;

    /**
     * Date the stage began.
     */
    private Date date;

    /**
     * Runtime, in seconds
     */
    private double elapsedSeconds;

    /**
     * Speed of the machine as produced by DiagnosticReport
     */
    private double singleCoreSpeed;

    /**
     * Speed of the machine as produced by DiagnosticReport
     */
    private double multiCoreSpeed;

    /**
     * Megabytes used
     */
    private double megabytesUsed;

    /**
     * True if the stage succeeded; default is true
     */
    private boolean succeeded = true;

    public StageDiagnostic(long runId, String stage, LanguageSet langs, double elapsedSeconds, double singleCoreSpeed, double multiCoreSpeed, double megabytesUsed) {
        this.runId = runId;
        this.stage = stage;
        this.date = new Date();
        this.langs = langs;
        this.elapsedSeconds = elapsedSeconds;
        this.singleCoreSpeed = singleCoreSpeed;
        this.multiCoreSpeed = multiCoreSpeed;
        this.megabytesUsed = megabytesUsed;
    }

    public Date getDate() {
        return date;
    }

    public String getStage() {
        return stage;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public double getMegabytesUsed() {
        return megabytesUsed;
    }

    public LanguageSet getLangs() {
        return langs;
    }

    public double getSingleCoreSpeed() {
        return singleCoreSpeed;
    }

    public double getMultiCoreSpeed() {
        return multiCoreSpeed;
    }

    public String getSystem() {
        return System.getProperty("os.name") + "; " +
                System.getProperty("os.version") + "; " +
                System.getProperty("os.arch");
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public boolean getSucceeded() {
        return succeeded;
    }

    public long getRunId() {
        return runId;
    }
}
