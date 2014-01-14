package org.wikapidia.wikidata;


import org.wikapidia.core.lang.Language;

/**
 * Created by bjhecht on 12/23/13.
 */
public class WikidataProperty {

    // universal id for this property
    private final int id;

    // These fields may be null unless resolved against the database
    private final Language language;
    private final String name;
    private final String description;

    public WikidataProperty(int id) {
        this.id = id;
        this.language = null;
        this.name = null;
        this.description = null;
    }

    public WikidataProperty(int id, Language language, String name, String description) {
        this.id = id;
        this.language = language;
        this.name = name;
        this.description = description;
    }

    public Language getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "WikidataProperty{" +
                "id=" + id +
                ", language=" + language +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
