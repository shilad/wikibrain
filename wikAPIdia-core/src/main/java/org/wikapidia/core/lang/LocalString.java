package org.wikapidia.core.lang;

/**
 * A language-specific string.
 * @author Shilad Sen
 */
public class LocalString {
    Language language;
    String string;

    public LocalString(Language language, String string) {
        this.language = language;
        this.string = string;
    }

    public Language getLanguage() {
        return language;
    }

    public String getString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalString that = (LocalString) o;

        if (!language.equals(that.language)) return false;
        if (!string.equals(that.string)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = language.hashCode();
        result = 31 * result + string.hashCode();
        return result;
    }
}
