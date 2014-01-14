package org.wikapidia.wikidata;

/**
 * @author Shilad Sen
 */
public class WikidataValue {

    public enum Type {ITEM, TIME, STRING, NOVALUE, SOMEVALUE, OTHER};

    private Type type;
    private String typeName;
    private Object value;

    public WikidataValue(Type type, String typeName, Object value) {
        this.type = type;
        this.typeName = typeName;
        this.value = value;
    }

    public WikidataValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String getTypeName() {
        return typeName;
    }
}
