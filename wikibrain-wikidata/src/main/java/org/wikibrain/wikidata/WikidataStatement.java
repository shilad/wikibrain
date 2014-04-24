package org.wikibrain.wikidata;

import java.io.Serializable;

/**
 * @author Shilad Sen
 * TODO: add refs, modifiers
 */
public class WikidataStatement implements Serializable {

    public static enum Rank {DEPRECATED, NORMAL, PREFERRED};

    private String id;
    private WikidataEntity item;
    private WikidataEntity property;
    private WikidataValue value;
    private Rank rank;


    public WikidataStatement(String id, WikidataEntity item, WikidataEntity property, WikidataValue value, Rank rank) {
        this.id = id;
        this.item = item;
        this.property = property;
        this.value = value;
        this.rank = rank;
    }

    public String getId() {
        return id;
    }

    public WikidataEntity getItem() {
        return item;
    }

    public WikidataEntity getProperty() {
        return property;
    }

    public WikidataValue getValue() {
        return value;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return "WikidataStatement{" +
                "id='" + id + '\'' +
                ", item=" + item +
                ", property=" + property +
                ", value=" + value +
                ", rank=" + rank +
                '}';
    }
}
