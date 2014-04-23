package org.wikibrain.core.model;

import org.wikibrain.core.lang.Language;

public class LocalArticle extends LocalPage {

    /**
     * Default Article Consturctor for non-redirect, non-disambig.
     * @param language
     * @param localId
     * @param title
     */
    public LocalArticle(Language language, int localId, Title title) {
        super(language, localId, title, NameSpace.ARTICLE);
    }

    /**
     * Article Constuctor for both disambig pages and redirect pages.
     * @param language
     * @param localId
     * @param title
     * @param disambig
     * @param redirect
     */
    public LocalArticle(Language language, int localId, Title title, boolean redirect, boolean disambig){
        super(language, localId, title, NameSpace.ARTICLE, redirect, disambig);
    }

    public LocalArticle(LocalPage page){
        super(page.getLanguage(), page.getLocalId(), page.getTitle(), NameSpace.ARTICLE);
    }
}
