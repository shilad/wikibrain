package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;

public class LocalArticle extends LocalPage {

    private boolean isDisambig;

    /**
     * Default Article Consturctor for non-redirect, non-disambig.
     * @param language
     * @param localId
     * @param title
     */
    public LocalArticle(Language language, int localId, Title title) {
        super(language, localId, title, NameSpace.ARTICLE);
        this.isDisambig = false;
    }

    /**
     * Article Constructor for non-disambig with the ability to set redirects.
     * @param language
     * @param localId
     * @param title
     * @param isRedirect
     */
    public LocalArticle(Language language, int localId, Title title, boolean isRedirect){
        super(language, localId, title, NameSpace.ARTICLE, isRedirect);
        this.isDisambig = false;
    }

    /**
     * Article Constuctor where you can set if it's a disambig or if its a redirect.

     * @param language
     * @param localId
     * @param title
     * @param isRedirect
     * @param isDisambig
     */
    public LocalArticle(Language language, int localId, Title title, boolean isRedirect, boolean isDisambig){
        super(language, localId, title, NameSpace.ARTICLE, isRedirect);
        this.isDisambig = isDisambig;
    }

    public boolean isDisambig() {
        return isDisambig;
    }
}
