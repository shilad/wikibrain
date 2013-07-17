package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

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

    private final int sourceUnivId;
    private final int destUnivId;

    public UniversalLink(int sourceUnivId, int destUnivId, int algorithmId, Multimap<Language, LocalLink> localEntities) {
        super(algorithmId, localEntities);
        this.sourceUnivId = sourceUnivId;
        this.destUnivId = destUnivId;
    }

    public UniversalLink(int sourceUnivId, int destUnivId, int algorithmId, LanguageSet languages) {
        super(algorithmId, languages);
        this.sourceUnivId = sourceUnivId;
        this.destUnivId = destUnivId;
    }

    public int getSourceUnivId() {
        return sourceUnivId;
    }

    public int getDestUnivId() {
        return destUnivId;
    }

    public Collection<LocalLink> getLocalLinks(Language language) {
        return new ArrayList<LocalLink>(getLocalEntities(language));
    }

    public static Multimap<Language, LocalLink> mergeMaps(Multimap<Language, LocalLink> map1, Multimap<Language, LocalLink> map2) {
        map1.putAll(map2);
        return map1;
    }
}
