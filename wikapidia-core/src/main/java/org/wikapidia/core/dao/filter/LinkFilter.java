package org.wikapidia.core.dao.filter;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalLink;

import java.util.ArrayList;
import java.util.Collection;

/**
 */
public class LinkFilter {

    private Collection<Short> langIds;
    private Collection<Short> locTypeIds;
    private Collection<Integer> sourceIds;
    private Collection<Integer> destIds;
    private Boolean isParseable;

    public LinkFilter() {
        langIds = null;
        sourceIds = null;
        destIds = null;
        locTypeIds = null;
        isParseable = null;
    }

    public Collection<Short> getLangIds() {
        return langIds;
    }

    public Collection<Short> getLocTypes() {
        return locTypeIds;
    }

    public Collection<Integer> getSourceIds() {
        return sourceIds;
    }

    public Collection<Integer> getDestIds() {
        return destIds;
    }

    public Boolean isParseable() {
        return isParseable;
    }

    public LinkFilter setLanguages(LanguageSet languages) {
        return setLanguages(languages.getLanguages());
    }

    public LinkFilter setLanguages(Collection<Language> languages) {
        Collection<Short> temp = new ArrayList<Short>();
        if (languages.isEmpty() || languages==null) {
            temp = null;
        }
        else {
            for (Language l : languages) {
                temp.add(l.getId());
            }
        }
        this.langIds = temp;
        return this;
    }

    public LinkFilter setLocTypeIds(LocalLink.LocationType[] locTypes) {
        return setLocTypeIds(Arrays.asList(locTypes));
    }

    public LinkFilter setLocTypeIds(Collection<LocalLink.LocationType> locTypes) {
        Collection<Short> temp = new ArrayList<Short>();
        if (locTypes.isEmpty() || locTypes==null) {
            temp = null;
        }
        else {
            for (LocalLink.LocationType lt : locTypes) {
                temp.add((short)lt.ordinal());
            }
        }
        this.locTypeIds = temp;
        return this;
    }

    public LinkFilter setSourceIds(Collection<Integer> sourceIds) {
        this.sourceIds = sourceIds;
        return this;
    }

    public LinkFilter setDestIds(Collection<Integer> destIds) {
        this.destIds = destIds;
        return this;
    }

    public LinkFilter setParseable(Boolean parseable) {
        isParseable = parseable;
        return this;
    }
}
