package org.wikibrain.loader.pipeline;

import org.wikibrain.core.lang.LanguageSet;

import java.util.Date;

/**
 * @author Shilad Sen
 */
public class StageDiagnostic {

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

    public StageDiagnostic(String stage, Date date, LanguageSet langs, double elapsedSeconds, double singleCoreSpeed, double multiCoreSpeed, double megabytesUsed) {
        this.stage = stage;
        this.date = date;
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
}
