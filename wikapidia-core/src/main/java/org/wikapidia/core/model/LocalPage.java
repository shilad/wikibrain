package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;

/**
 */
public class LocalPage {

    protected final Language language;
    protected final int localId;
    protected final Title title;
    protected final NameSpace nameSpace;
    protected final boolean isRedirect;
    protected final boolean isDisambig;

    /**
     * Default for NON-redirect pages.
     * @param language
     * @param localId
     * @param title
     * @param nameSpace
     */
    public LocalPage(Language language, int localId, Title title, NameSpace nameSpace){
        this.language = language;
        this.localId = localId;
        this.title = title;
        this.nameSpace = nameSpace;
        isRedirect = false;
        isDisambig = false;
    }

    /**
     * Ability to set redirect pages.
     * @param language
     * @param localId
     * @param title
     * @param nameSpace
     * @param redirect
     */
    public LocalPage(Language language, int localId, Title title, NameSpace nameSpace, boolean redirect, boolean disambig) {
        this.language = language;
        this.localId = localId;
        this.title = title;
        this.nameSpace = nameSpace;
        isRedirect = redirect;
        isDisambig = disambig;
    }

    public int getLocalId() {
        return localId;
    }

    public Title getTitle() {
        return title;
    }

    public Language getLanguage() {
        return language;
    }

    public NameSpace getNameSpace() {
        return nameSpace;
    }

    public boolean isDisambig() {
        return isDisambig;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public int hashCode(){
        return (language.getId() + "_" + localId).hashCode(); //non-optimal
    }

    public boolean equals(Object o){
        if (o instanceof LocalPage){
            LocalPage input = (LocalPage)o;
            return (input.getLanguage().equals(this.getLanguage()) &&
                    input.getLocalId() == this.getLocalId()
            );
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "LocalPage{" +
                "nameSpace=" + nameSpace +
                ", title=" + title +
                ", localId=" + localId +
                ", language=" + language +
                '}';
    }
}