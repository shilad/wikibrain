package org.wikapidia.sr;

import java.util.List;

public class Explanation {
    private String format;
    private List<Object> information;

    /**
     * Used to make a human-readable explanations of sr measures
     * @param format a string with ?s where the objects should be placed
     * @param information the objects to be placed in the format string
     */
    public Explanation(String format, List information){
        this.format = format;
        this.information = information;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<Object> getInformation() {
        return information;
    }

    public void setInformation(List<Object> information) {
        this.information = information;
    }
}
