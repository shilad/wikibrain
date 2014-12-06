package org.wikibrain.wikidata;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;

import java.io.Serializable;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class WikidataEntity implements Serializable {
    public static enum Type {
        ITEM('Q'), PROPERTY('P');

        public char code;
        Type(char code) { this.code = code; }

        public static Type getByCode(char code) {
            code = Character.toUpperCase(code);
            for (Type type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown type code: " + code);
        }
    }

    private final Type type;
    private final int id;

    private Map<Language, String> labels = new LinkedHashMap<Language, String>();
    private Map<Language, String> descriptions = new LinkedHashMap<Language, String>();
    private Map<Language, List<String>> aliases = new LinkedHashMap<Language, List<String>>();
    private List<WikidataStatement> statements = new ArrayList<WikidataStatement>();

    /**
     * ID must be a "P" or "Q" id. E.g. "P34"
     * @param id
     */
    public WikidataEntity(String id) {
        if (id.toUpperCase().startsWith("P")) {
            type = Type.PROPERTY;
            this.id = Integer.valueOf(id.substring(1));
        } else if (id.toUpperCase().startsWith("Q")) {
            type = Type.ITEM;
            this.id = Integer.valueOf(id.substring(1));
        } else {
            throw new IllegalArgumentException("Invalid wikidata id: " + id);
        }
    }
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

    public Map<String, List<WikidataStatement>> getStatementsInLanguage(Language language) {
        Map<String, List<WikidataStatement>> inLang = new HashMap<String, List<WikidataStatement>>();
        for (WikidataStatement s : statements) {
            String label = s.getProperty().getLabels().get(language);
            if (label != null) {
                if (!inLang.containsKey(label)) {
                    inLang.put(label, new ArrayList<WikidataStatement>());
                }
                inLang.get(label).add(s);
            }
        }
        return inLang;
    }

    /**
     * Prunes a WikiData entity to the specified languages.
     * Returns true IFF a label, description, or alias exists in one of the specified languages.
     * @param langs
     * @return
     */
    public boolean prune(LanguageSet langs) {
        pruneSet(aliases.keySet(), langs);
        pruneSet(descriptions.keySet(), langs);
        pruneSet(labels.keySet(), langs);
        return (aliases.size() > 0 || descriptions.size() > 0 || labels.size() > 0);
    }

    private void pruneSet(Collection<Language> set, LanguageSet keepers) {
        Iterator<Language> iter = set.iterator();
        while (iter.hasNext()) {
            Language l = iter.next();
            if (!keepers.containsLanguage(l)) {
                iter.remove();
            }
        }
    }

    @Override
    public int hashCode() {
        return type.hashCode() + 37 * id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WikidataEntity that = (WikidataEntity) o;

        if (id != that.id) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public String toString() {
        String name;
        Language en = Language.getByLangCode("en");
        if (labels.containsKey(en)) {
            name = labels.get(en);
        } else if (labels.isEmpty()) {
            name = "unknown";
        } else {
            name = labels.values().iterator().next();
        }
        return "WikidataEntity{" +
                "type=" + type +
                ", id=" + id +
                ", name=" + name +
                '}';
    }
}
