package org.wikapidia.wikidata;

/**
 * @author Shilad Sen
 * TODO: add refs, modifiers
 */
public class WikidataStatement {

    public static enum Rank {DEPRECATED, NORMAL, PREFERRED};

    private String id;
    private WikidataItem item;
    private WikidataProperty property;
    private WikidataValue value;
    private Rank rank;


    public WikidataStatement(String id, WikidataItem item, WikidataProperty property, WikidataValue value, Rank rank) {
        this.id = id;
        this.item = item;
        this.property = property;
        this.value = value;
        this.rank = rank;
    }

    public String getId() {
        return id;
    }

    public WikidataItem getItem() {
        return item;
    }

    public WikidataProperty getProperty() {
        return property;
    }

    public WikidataValue getValue() {
        return value;
    }

    public Rank getRank() {
        return rank;
    }
}
