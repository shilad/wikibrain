package org.wikibrain.core.lang;

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

        return language.equals(that.language) && string.equals(that.string);

    }

    @Override
    public int hashCode() {
        int result = language.hashCode();
        result = 31 * result + string.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LocalString{" +
                "language=" + language +
                ", string='" + string + '\'' +
                '}';
    }
}
