package org.wikibrain.core.model;

import org.wikibrain.core.lang.Language;

import java.util.Date;

/**
 * A MetaInfo accumulates information about the number of records
 * and errors associated with some component.
 *
* @author Shilad Sen
*/
public class MetaInfo {
    // The internal database id
    private long id = -1;

    // The last count of (records + errors) written to the database.
    // Can be used internally when delaying writes
    private int lastWrite = 0;

    // component + language uniquely define a meta info. language can be null.
    private final Class component;
    private final Language language;

    // accumulators
    private int numRecords = 0;
    private int numErrors = 0;
    private Date lastUpdated = null;

    public MetaInfo(Class component) {
        this(component, null);
    }

    public MetaInfo(Class component, Language language) {
        this(component, language, 0, 0, null);
    }

    public MetaInfo(Class component, Language language, int numRecords, int numErrors, Date lastUpdated) {
        this(component, language, -1, numRecords, numErrors, lastUpdated);
    }

    public MetaInfo(Class component, Language language, long id, int numRecords, int numErrors, Date lastUpdated) {
        this.component = component;
        this.language = language;
        this.id = id;
        this.numRecords = numRecords;
        this.numErrors = numErrors;
        this.lastUpdated = lastUpdated;
    }

    public int incrementNumRecords() {
        return incrementNumRecords(1);
    }

    public synchronized int incrementNumErrors() {
        lastUpdated = new Date();
        return ++numErrors;
    }


    public synchronized  int incrementNumRecords(int n) {
        lastUpdated = new Date();
        numRecords += n;
        return numRecords;
    }

    public long getId() {
        return id;
    }

    public int getLastWrite() {
        return lastWrite;
    }

    public Language getLanguage() {
        return language;
    }

    public int getNumRecords() {
        return numRecords;
    }

    public int getNumErrors() {
        return numErrors;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public int numNotWritten() {
        return numErrors + numRecords - lastWrite;
    }

    public void markAsWritten() {
        lastWrite = numErrors + numRecords;
    }

    public Class getComponent() {
        return component;
    }

    public synchronized void clear() {
        numErrors = 0;
        numRecords = 0;
        lastWrite = 0;
        lastUpdated = null;
    }

    /**
     * Merges the accumulated values of the passed-in info with the current values.
     * @param info
     */
    public void merge(MetaInfo info) {
        if (!info.component.equals(component)) {
            throw new IllegalArgumentException();
        }
        numErrors += info.numErrors;
        numRecords += info.numRecords;
        lastWrite = numErrors + numRecords;

        if (lastUpdated == null) {
            lastUpdated = info.lastUpdated;
        } else if (info.lastUpdated != null && info.lastUpdated.compareTo(lastUpdated) > 0) {
            lastUpdated = info.lastUpdated;
        }
    }

    @Override
    public String toString() {
        return "MetaInfo{" +
                "component=" + component +
                ", language=" + language +
                ", numRecords=" + numRecords +
                ", numErrors=" + numErrors +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
