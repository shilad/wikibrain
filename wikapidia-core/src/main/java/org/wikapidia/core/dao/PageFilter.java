package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A helper class for specifying complex queries.  To use, call the static get() method to create an instance,
 * than call the various set methods in a chain to set the constraints. For example, a call might look like:
 *
 * PageFilter pf = PageFilter.get().
 *          setLanguages(languageSet).
 *          setNameSpace(nameSpaces).
 *          setRedirect(true).
 *          setDisambig(false);
 */
public class PageFilter {
    private final Collection<Short> langIds;
    private final Collection<Short> nsIds;
    private final Boolean isRedirect;
    private final Boolean isDisambig;

    public PageFilter() {
        langIds = null;
        nsIds = null;
        isRedirect = null;
        isDisambig = null;
    }

    private PageFilter(Collection<Short> langIds, Collection<Short> nsIds, Boolean redirect, Boolean disambig) {
        this.langIds = langIds;
        this.nsIds = nsIds;
        isRedirect = redirect;
        isDisambig = disambig;
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

    public PageFilter setLanguages(LanguageSet languages) {
        Collection<Short> temp = new ArrayList<Short>();
        if (languages.getNumberOfLanguages()==0 || languages==null) {
            temp = null;
        }
        else {
            for (Language l : languages) {
                temp.add(l.getId());
            }
        }
        return new PageFilter(
                temp,
                getNameSpaces(),
                isRedirect(),
                isDisambig()
        );
    }

    public PageFilter setLanguages(Collection<Language> languages) {
        Collection<Short> temp = new ArrayList<Short>();
        if (languages.isEmpty() || languages==null) {
            temp = null;
        }
        else {
            for (Language l : languages) {
                temp.add(l.getId());
            }
        }
        return new PageFilter(
                temp,
                getNameSpaces(),
                isRedirect(),
                isDisambig()
        );
    }

    public PageFilter setNameSpaces(Collection<NameSpace> nameSpaces) {
        Collection<Short> temp = new ArrayList<Short>();
        if (nameSpaces.isEmpty() || nameSpaces==null) {
            temp = null;
        }
        else {
            for (NameSpace ns : nameSpaces) {
                temp.add(ns.getArbitraryId());
            }
        }
        return new PageFilter(
                getLanguages(),
                temp,
                isRedirect(),
                isDisambig()
        );
    }

    public PageFilter setRedirect(Boolean redirect) {
        return new PageFilter(
                getLanguages(),
                getNameSpaces(),
                redirect,
                isDisambig()
        );
    }

    public PageFilter setDisambig(Boolean disambig) {
        return new PageFilter(
                getLanguages(),
                getNameSpaces(),
                isRedirect(),
                disambig
        );
    }
}
