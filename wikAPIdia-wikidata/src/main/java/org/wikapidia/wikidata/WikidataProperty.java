package org.wikapidia.wikidata;


import org.wikapidia.core.lang.Language;

/**
 * Created by bjhecht on 12/23/13.
 */
public class WikidataProperty {

    private Language language;
    private String name;
    private String description;

    public WikidataProperty(Language language, String name, String description) {
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
                "language=" + language +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
