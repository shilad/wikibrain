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

    private Title title;
    private String body;
    private Date lastEdit;

    private Language lang;
    private int revisionId;
    private int pageId;

    private PageType type;

    public RawPage(int pageId, int revisionId, String title, String body, Date lastEdit, PageType type, Language lang) {
        this.title = new Title(title, false, LanguageInfo.getByLanguage(lang));
        this.body = body;
        this.lastEdit = lastEdit;
        this.type = type;
        this.lang = lang;
        this.revisionId = revisionId;
        this.pageId = pageId;
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

    public PageType getType() {
        return type;
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

    public String toString(){
        return String.format("%s / %s (%s)", this.getTitle(), this.pageId, lang.getLangCode());
    }
}
