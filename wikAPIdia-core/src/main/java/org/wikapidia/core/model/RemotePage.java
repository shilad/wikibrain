package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;

/**
 */
public class RemotePage {

    protected final Language language;
    protected final int remoteId;
    protected final Title title;
    protected final NameSpace nameSpace;
    protected final boolean isRedirect;
    protected final boolean isDisambig;

    /**
     * Default for NON-redirect pages.
     * @param language
     * @param remoteId
     * @param title
     * @param nameSpace
     */
    public RemotePage(Language language, int remoteId, Title title, NameSpace nameSpace){
        this.language = language;
        this.remoteId = remoteId;
        this.title = title;
        this.nameSpace = nameSpace;
        isRedirect = false;
        isDisambig = false;
    }

    /**
     * Ability to set redirect pages.
     * @param language
     * @param remoteId
     * @param title
     * @param nameSpace
     * @param redirect
     */
    public RemotePage(Language language, int remoteId, Title title, NameSpace nameSpace, boolean redirect, boolean disambig) {
        this.language = language;
        this.remoteId = remoteId;
        this.title = title;
        this.nameSpace = nameSpace;
        isRedirect = redirect;
        isDisambig = disambig;
    }

    public int getRemoteId() {
        return remoteId;
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
        return (language.getId() + "_" + remoteId).hashCode(); //non-optimal
    }

    public LocalId toLocalId() {
        return new LocalId(language, remoteId);
    }

    public boolean equals(Object o){
        if (o instanceof RemotePage){
            RemotePage input = (RemotePage)o;
            return (input.getLanguage().equals(this.getLanguage()) &&
                    input.getRemoteId() == this.getRemoteId()
            );
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "RemotePage{" +
                "nameSpace=" + nameSpace +
                ", title=" + title +
                ", remoteId=" + remoteId +
                ", language=" + language +
                '}';
    }
}