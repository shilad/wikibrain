package org.wikibrain.core.model;

import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains a single page's data from Wikipedia's Xml Dump with no processing.
 * You probably don't want to use this class unless you are parsing or need full text.
 */
public class RawPage {
    private static final Logger LOG = LoggerFactory.getLogger(RawPage.class);

    private final Title title;
    private final String body;
    private final Date lastEdit;

    private final Language lang;
    private final int revisionId;
    private final int localId;

    private final NameSpace namespace;
    private final boolean isRedirect;
    private final boolean isDisambig;
    private String redirectTitle = null;

    // Wikidata assigns these two fields
    private String model = null;
    private String format = null;

    public RawPage(int localId, int revisionId, String title, String body, Date lastEdit, Language lang, NameSpace namespace) {
        this.title = new Title(title, LanguageInfo.getByLanguage(lang));
        this.body = body;
        this.lastEdit = lastEdit;
        this.namespace = namespace;
        this.lang = lang;
        this.revisionId = revisionId;
        this.localId = localId;
        isRedirect = false;
        isDisambig = false;
    }

    public String getRedirectTitle() {
        return redirectTitle;
    }

    public void setRedirectTitle(String redirectTitle) {
        this.redirectTitle = redirectTitle;
    }

    public RawPage(int localId, int revisionId, String title, String body, Date lastEdit, Language lang, NameSpace namespace,
                   boolean redirect, boolean disambig, String redirectTitle) {
        this.title = new Title(title, LanguageInfo.getByLanguage(lang));
        this.body = body;
        this.lastEdit = lastEdit;
        this.lang = lang;
        this.revisionId = revisionId;
        this.localId = localId;
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

    public Language getLanguage() {
        return lang;
    }

    public int getRevisionId() {
        return revisionId;
    }

    public int getLocalId() {
        return localId;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Returns a plain text output of the body of this RawPage
     * @return
     */
    public String getPlainText() {
        return getPlainText(false);
    }
    /**
     * Returns a plain text output of the body of this RawPage
     * @return
     */
    public String getPlainText(boolean includeTemplates) {
        if (body.isEmpty()) {
            return "";
        } else {
            MediaWikiParserFactory factory = new MediaWikiParserFactory();
            if (!includeTemplates) {
                factory.setTemplateParserClass(FlushTemplates.class);
            }
            return factory.createParser().parse(body).getText();
        }
    }

    public String toString(){
        return String.format("%s / %s (%s)", this.getTitle(), this.localId, lang.getLangCode());
    }
}
