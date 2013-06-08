package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;

public class LocalArticle extends LocalPage {

    public LocalArticle(Language language, int localId, Title title) {
        super(language, localId, title, PageType.ARTICLE);
    }
}
