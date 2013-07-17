package org.wikapidia.core.lang;

import org.wikapidia.core.model.LocalPage;

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

    public LocalPage asLocalPage(){
        return new LocalPage(
                language,
                id,
                null,
                null
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalId localId = (LocalId) o;

        return id == localId.id && language.equals(localId.language);

    }

    @Override
    public int hashCode() {
        int result = language.hashCode();
        result = 31 * result + id;
        return result;
    }
}
