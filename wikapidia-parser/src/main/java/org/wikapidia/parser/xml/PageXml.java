package org.wikapidia.parser.xml;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.PageType;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Contains a single page's data from Wikipedia's Xml Dump with no processing.
 */
public class PageXml {
    private static final Logger LOG = Logger.getLogger(PageXml.class.getName());

    private String title;
    private String body;
    private Date lastEdit;

    private Language lang;
    private Long startByte, endByte;    // TODO: Are these necessary?
    private int revisionId;
    private int pageId;

    private PageType type;

    public PageXml(int pageId, int revisionId, String title, String body, Date lastEdit, PageType type, Language lang, Long startByte, Long endByte) {
        this.title = title;
        this.body = body;
        this.lastEdit = lastEdit;
        this.type = type;
        this.lang = lang;
        this.startByte = startByte;
        this.endByte = endByte;
        this.revisionId = revisionId;
        this.pageId = pageId;
    }

    public String getTitle() {
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

    public Long getStartByte() {
        return startByte;
    }

    public Long getEndByte() {
        return endByte;
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
