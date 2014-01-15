package org.wikapidia.wikidata;

import org.wikapidia.core.lang.Language;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class WikidataEntity {
    public static enum Type {
        ITEM('Q'), PROPERTY('P');

        public char code;
        Type(char code) { this.code = code; }
    }

    private Type type;
    private int id;

    private Map<Language, String> labels = new LinkedHashMap<Language, String>();
    private Map<Language, String> descriptions = new LinkedHashMap<Language, String>();
    private Map<Language, List<String>> aliases = new LinkedHashMap<Language, List<String>>();
    private List<WikidataStatement> statements = new ArrayList<WikidataStatement>();

    public WikidataEntity(Type type, int id) {
        this.type = type;
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public String getStringId() {
        return type.code + "" + id;
    }

    public Map<Language, String> getLabels() {
        return labels;
    }

    public Map<Language, String> getDescriptions() {
        return descriptions;
    }

    public Map<Language, List<String>> getAliases() {
        return aliases;
    }

    public List<WikidataStatement> getStatements() {
        return statements;
    }
}
