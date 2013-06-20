package org.wikapidia.core.dao.filter;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A helper class for specifying complex queries.  To use, call the static get() method to create an instance,
 * than call the various set methods in a chain to set the constraints. For example, a call might look like:
 *
 * LocalPageFilter pf = new LocalPageFilter.
 *          setLanguages(languageSet).
 *          setNameSpace(nameSpaces).
 *          setRedirect(true).
 *          setDisambig(false);
 */
public class LocalPageFilter {

    private Collection<Short> langIds;
    private Collection<Short> nsIds;
    private Boolean isRedirect;
    private Boolean isDisambig;

    public LocalPageFilter() {
        langIds = null;
        nsIds = null;
        isRedirect = null;
        isDisambig = null;
    }

    public Collection<Short> getLanguages() {
        return langIds;
    }

    public Collection<Short> getNameSpaces() {
        return nsIds;
    }

    public Boolean isRedirect() {
        return isRedirect;
    }

    public Boolean isDisambig() {
        return isDisambig;
    }

    public LocalPageFilter setLanguages(LanguageSet languages) {
        return setLanguages(languages.getLanguages());
    }

    public LocalPageFilter setLanguages(Collection<Language> languages) {
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

    public LocalPageFilter setNameSpaces(Collection<NameSpace> nameSpaces) {
        Collection<Short> temp = new ArrayList<Short>();
        if (nameSpaces.isEmpty() || nameSpaces==null) {
            temp = null;
        }
        else {
            for (NameSpace ns : nameSpaces) {
                temp.add(ns.getArbitraryId());
            }
        }
        this.nsIds = temp;
        return this;
    }

    public LocalPageFilter setRedirect(Boolean redirect) {
        this.isRedirect = redirect;
        return this;
    }

    public LocalPageFilter setDisambig(Boolean disambig) {
        this.isDisambig = disambig;
        return this;
    }
}
