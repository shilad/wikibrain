package org.wikibrain.wikidata;

import org.wikibrain.core.lang.Language;

import java.util.*;

/**
 * @author Shilad Sen
 */
public class WikidataFilter {
    private Collection<Language> langs;

    // Entity types
    private Collection<WikidataEntity.Type> entityTypes;
    private Collection<Integer> entityIds;

    // For statements, but not entities
    private Collection<Integer> propertyIds;
    private Collection<WikidataStatement.Rank> ranks;
    private Collection<WikidataValue> values;

    public WikidataFilter() {
    }

    public Collection<Language> getLangs() {
        return langs;
    }

    public Collection<Short> getLangIds() {
        if (langs == null) {
            return null;
        }
        Set<Short> ids = new HashSet<Short>();
        for (Language lang : langs) {
            ids.add(lang.getId());
        }
        return ids;
    }

    public Collection<WikidataEntity.Type> getEntityTypes() {
        return entityTypes;
    }

    public Collection<String> getEntityTypeCodes() {
        if (entityTypes == null) {
            return null;
        }
        Set<String> typeCodes = new HashSet<String>();
        for (WikidataEntity.Type type : entityTypes) {
            typeCodes.add(Character.toString(type.code));
        }
        return typeCodes;
    }

    public Collection<WikidataValue> getValues() {
        return values;
    }

    public Collection<Integer> getEntityIds() {
        return entityIds;
    }

    public Collection<Integer> getPropertyIds() {
        return propertyIds;
    }

    public Collection<WikidataStatement.Rank> getRanks() {
        return ranks;
    }

    public Collection<Short> getRankOrdinals() {
        if (ranks == null) {
            return null;
        }
        Set<Short> ordinals = new HashSet<Short>();
        for (WikidataStatement.Rank rank : ranks) {
            ordinals.add((short)rank.ordinal());
        }
        return ordinals;
    }

    /**
     * Utility class to build a filter
     */
    public static class Builder {
        private WikidataFilter filter = new WikidataFilter();

        public Builder withLanguage(Language language) {
            filter.langs = Arrays.asList(language);
            return this;
        }

        public Builder withLanguages(Collection<Language> languages) {
            filter.langs = languages;
            return this;
        }

        public Builder withEntityType(WikidataEntity.Type type) {
            filter.entityTypes = Arrays.asList(type);
            return this;
        }

        public Builder withEntityType(Collection<WikidataEntity.Type> types) {
            filter.entityTypes = types;
            return this;
        }

        public Builder withEntityId(int id) {
            filter.entityIds = Arrays.asList(id);
            return this;
        }

        public Builder withEntityIds(Collection<Integer> ids) {
            filter.entityIds = ids;
            return this;
        }

        public Builder withPropertyId(int id) {
            filter.propertyIds = Arrays.asList(id);
            return this;
        }

        public Builder withPropertyIds(Collection<Integer> ids) {
            filter.propertyIds = ids;
            return this;
        }

        public Builder withRank(WikidataStatement.Rank rank) {
            filter.ranks = Arrays.asList(rank);
            return this;
        }

        public Builder withRanks(Collection<WikidataStatement.Rank> ranks) {
            filter.ranks = ranks;
            return this;
        }

        public Builder withValue(WikidataValue value) {
            filter.values = Arrays.asList(value);
            return this;
        }

        public Builder withValues(Collection<WikidataValue> values) {
            filter.values = values;
            return this;
        }

        public WikidataFilter build() {
            return filter;
        }
    }
}
