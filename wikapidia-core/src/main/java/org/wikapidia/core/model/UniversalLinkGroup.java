package org.wikapidia.core.model;

import org.wikapidia.core.lang.LanguageSet;

import java.util.Map;

/**
 */
public class UniversalLinkGroup {

    private final Map<Integer, UniversalLink> links;
    private final boolean isOutlink;
    private final int univId;
    private final int algorithmId;
    private final LanguageSet languageSet;

    public UniversalLinkGroup(Map<Integer, UniversalLink> links, boolean outlink, int univId, int algorithmId, LanguageSet languageSet) {
        this.links = links;
        isOutlink = outlink;
        this.univId = univId;
        this.algorithmId = algorithmId;
        this.languageSet = languageSet;
    }
}
