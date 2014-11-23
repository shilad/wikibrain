package org.wikibrain.core.model;

import com.google.common.collect.Multimap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * A class to represent a Universal Link.
 *
 * @author Ari Weiland
 *
 */
public class UniversalLink extends AbstractUniversalEntity<LocalLink> {

    private final int sourceId;
    private final int destId;

    public UniversalLink(int sourceId, int destId, int algorithmId, Multimap<Language, LocalLink> localEntities) {
        super(algorithmId, localEntities);
        this.sourceId = sourceId;
        this.destId = destId;
    }

    public UniversalLink(int sourceId, int destId, int algorithmId, LanguageSet languages) {
        super(algorithmId, languages);
        this.sourceId = sourceId;
        this.destId = destId;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getDestId() {
        return destId;
    }

    public Collection<LocalLink> getLocalLinks(Language language) {
        return new ArrayList<LocalLink>(getLocalEntities(language));
    }

    public static Multimap<Language, LocalLink> mergeMaps(Multimap<Language, LocalLink> map1, Multimap<Language, LocalLink> map2) {
        map1.putAll(map2);
        return map1;
    }
}
