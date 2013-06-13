package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;

public class LocalCategory extends LocalPage {

    public LocalCategory(Language language, int localId, Title title) {
        super(language, localId, title, NameSpace.CATEGORY);
    }

    public LocalCategory(Language language, int localId, Title title, boolean redirect) {
        super(language, localId, title, NameSpace.CATEGORY, redirect, false);
    }
}