package org.wikapidia.wikidata;

import org.wikapidia.core.model.RawPage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class WikidataRawRecord {
    private RawPage rawPage;
    private Map<String, String> labels = new LinkedHashMap<String, String>();
    private Map<String, String> descriptions = new LinkedHashMap<String, String>();
    private Map<String, List<String>> aliases = new LinkedHashMap<String, List<String>>();

    // TODO: handle links

    private String entityType;
    private int entityId;

    public WikidataRawRecord(RawPage rawPage) {
        this.rawPage = rawPage;
    }

    public RawPage getRawPage() {
        return rawPage;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public Map<String, List<String>> getAliases() {
        return aliases;
    }

    public String getEntityType() {
        return entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }
}
