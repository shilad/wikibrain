package org.wikibrain.core.model;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;

/**
 * @author Shilad Sen
 */
public class InterLanguageLink {
    private LocalId source;
    private LocalId dest;

    public InterLanguageLink(Language sourceLang, int sourceId, Language destLang, int destId) {
        this(new LocalId(sourceLang, sourceId), new LocalId(destLang, destId));
    }

    public InterLanguageLink(LocalId source, LocalId dest) {
        this.source = source;
        this.dest = dest;
    }

    public LocalId getSource() {
        return source;
    }

    public LocalId getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return "InterLanguageLink{" +
                "source=" + source +
                ", dest=" + dest +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InterLanguageLink that = (InterLanguageLink) o;

        if (!dest.equals(that.dest)) return false;
        if (!source.equals(that.source)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + dest.hashCode();
        return result;
    }
}
