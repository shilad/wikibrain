package org.wikapidia.core.model;

import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
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

    private final NameSpace namespace;
    private final boolean isRedirect;
    private final boolean isDisambig;
    private String redirectTitle = null;

    public RawPage(int pageId, int revisionId, String title, String body, Date lastEdit, Language lang, NameSpace namespace) {
        this.title = new Title(title, LanguageInfo.getByLanguage(lang));
        this.body = body;
        this.lastEdit = lastEdit;
        this.namespace = namespace;
        this.lang = lang;
        this.revisionId = revisionId;
        this.pageId = pageId;
        isRedirect = false;
        isDisambig = false;
    }

    public String getRedirectTitle() {
        return redirectTitle;
    }

    public void setRedirectTitle(String redirectTitle) {
        this.redirectTitle = redirectTitle;
    }

    public RawPage(int pageId, int revisionId, String title, String body, Date lastEdit, Language lang, NameSpace namespace,
                   boolean redirect, boolean disambig, String redirectTitle) {
        this.title = new Title(title, LanguageInfo.getByLanguage(lang));
        this.body = body;
        this.lastEdit = lastEdit;
        this.lang = lang;
        this.revisionId = revisionId;
        this.pageId = pageId;
        this.namespace = namespace;
        isRedirect = redirect;
        isDisambig = disambig;
        this.redirectTitle = redirectTitle;
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

    public NameSpace getNamespace() {
        return namespace;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public boolean isDisambig() {
        return isDisambig;
    }

    /**
     * Returns a plain text output of the body of this RawPage
     * @return
     */
    public String getPlainText() {
        if (body.isEmpty()) {
            return ""; // TODO: this is a bad workaround, we should fix it
        } else {
            return new MediaWikiParserFactory().createParser().parse(body).getText();
        }
    }

    public String toString(){
        return String.format("%s / %s (%s)", this.getTitle(), this.pageId, lang.getLangCode());
    }
}
