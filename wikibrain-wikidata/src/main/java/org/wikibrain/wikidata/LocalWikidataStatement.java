package org.wikibrain.wikidata;

import org.wikibrain.core.lang.Language;

/**
 * A wrapper for a Wikidata statement that encodes a human interpretable String description.
 *
 * @author Shilad Sen
 */
public class LocalWikidataStatement {
    private Language lang;
    private WikidataStatement statement;

    private String fullStatement;
    private String item;
    private String property;
    private String value;

    public LocalWikidataStatement(Language lang, WikidataStatement statement, String fullStatement, String item, String property, String value) {
        this.lang = lang;
        this.statement = statement;
        this.fullStatement = fullStatement;
        this.item = item;
        this.property = property;
        this.value = value;
    }

    public Language getLang() {
        return lang;
    }

    public WikidataStatement getStatement() {
        return statement;
    }

    public String getFullStatement() {
        return fullStatement;
    }

    public String getItem() {
        return item;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return fullStatement;
    }
}
