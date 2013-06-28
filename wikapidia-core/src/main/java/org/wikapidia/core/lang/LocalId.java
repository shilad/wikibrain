package org.wikapidia.core.lang;

/**
 * A language-specific id.
 * @author Shilad Sen
 */
public class LocalId {
    private Language language;
    private int id;

    public LocalId(Language language, int id) {
        this.language = language;
        this.id = id;
    }

    public Language getLanguage() {
        return language;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalId localId = (LocalId) o;

        if (id != localId.id) return false;
        if (!language.equals(localId.language)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = language.hashCode();
        result = 31 * result + id;
        return result;
    }
}
