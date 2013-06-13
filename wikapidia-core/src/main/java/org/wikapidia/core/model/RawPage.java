package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Contains a single page's data from Wikipedia's Xml Dump with no processing.
 * You probably don't want to use this class unless you are parsing or need full text.
 */
public class RawPage {
    private static final Logger LOG = Logger.getLogger(RawPage.class.getName());

    private final Title title;
    private final String body;
    private final Date lastEdit;

    private final Language lang;
    private final int revisionId;
    private final int pageId;

    private final NameSpace type;
    private final boolean isRedirect;

    public RawPage(int pageId, int revisionId, String title, String body, Date lastEdit, Language lang, NameSpace type) {
        this.title = new Title(title, LanguageInfo.getByLanguage(lang));
        this.body = body;
        this.lastEdit = lastEdit;
        this.type = type;
        this.lang = lang;
        this.revisionId = revisionId;
        this.pageId = pageId;
        isRedirect = false;
    }

    public RawPage(int pageId, int revisionId, String title, String body, Date lastEdit, Language lang, NameSpace type, boolean redirect) {
        this.title = new Title(title, LanguageInfo.getByLanguage(lang));
        this.body = body;
        this.lastEdit = lastEdit;
        this.lang = lang;
        this.revisionId = revisionId;
        this.pageId = pageId;
        this.type = type;
        isRedirect = redirect;
    }

    public Title getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Date getLastEdit() {
        return lastEdit;
    }

    public Language getLang() {
        return lang;
    }

    public int getRevisionId() {
        return revisionId;
    }

    public int getPageId() {
        return pageId;
    }

    public NameSpace getType() {
        return type;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public String toString(){
        return String.format("%s / %s (%s)", this.getTitle(), this.pageId, lang.getLangCode());
    }
}
