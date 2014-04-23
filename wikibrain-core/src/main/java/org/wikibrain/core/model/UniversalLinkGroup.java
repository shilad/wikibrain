package org.wikibrain.core.model;

import org.wikibrain.core.lang.LanguageSet;

import java.util.Iterator;
import java.util.Map;

/**
 *
 * A convienent wrapper for a group of Universal Inlinks or Outlinks.
 *
 * @author Ari Weiland
 *
 */
public class UniversalLinkGroup implements Iterable<UniversalLink> {
    //If isOutlink, maps dest id to UniversalLinks
    //Otherwise maps source id to UniversalLinks
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

    public Map<Integer, UniversalLink> getLinks() {
        return links;
    }

    public boolean isOutlink() {
        return isOutlink;
    }

    public int getUnivId() {
        return univId;
    }

    public int getAlgorithmId() {
        return algorithmId;
    }

    public LanguageSet getLanguageSet() {
        return languageSet;
    }

    @Override
    public Iterator<UniversalLink> iterator() {
        return links.values().iterator();
    }
}
