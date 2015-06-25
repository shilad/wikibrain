package org.wikibrain.webapi;

import org.apache.commons.lang3.text.WordUtils;
import org.wikibrain.core.lang.Language;

import java.util.HashMap;
import java.util.Map;


/**
 * Represents a single type of web entity.
 *
 * @author Shilad Sen
 */
public class WebEntity {
    public enum Type {
        TITLE,
        PHRASE,
        ARTICLE_ID,
        CONCEPT_ID ;

        @Override
        public String toString() {
            return WordUtils.uncapitalize(
                    WordUtils.capitalizeFully(name(), new char[]{'_'})
                            .replaceAll("_", ""));
        }
    };

    private Type type;
    private Language lang;
    private String title;
    private String phrase;
    private int articleId = -1;
    private int conceptId = -1;

    public WebEntity(Language lang, WikiBrainWebRequest req) {
        this(lang, "", req);
    }

    public WebEntity(Language lang, String suffix, WikiBrainWebRequest req) {
        String errorMessage = "Must specify exactly one of the following params:";
        for (Type t : Type.values()) {
            errorMessage += " " + t + suffix;
        }
        String value = null;
        for (Type t : Type.values()) {
            if (req.hasParam(t + suffix)) {
                if (value != null) throw new WikiBrainWebException(errorMessage);
                type = t;
                value = req.getParam(t + suffix);
            }
        }
        if (value == null) throw new WikiBrainWebException(errorMessage);
        this.lang = lang;
        switch (type) {
            case TITLE: this.title = value; break;
            case PHRASE: this.phrase = value; break;
            case ARTICLE_ID: this.articleId = Integer.valueOf(value); break;
            case CONCEPT_ID: this.conceptId = Integer.valueOf(value); break;
        }
    }

    public Type getType() {
        return type;
    }

    public Language getLang() {
        return lang;
    }

    public String getTitle() {
        return title;
    }

    public String getPhrase() {
        return phrase;
    }

    public int getArticleId() {
        return articleId;
    }

    public void setArticleId(int articleId) {
        this.articleId = articleId;
    }

    public int getConceptId() {
        return conceptId;
    }

    public String toString() {
        return "{" + type + ": " + getValue().toString() + "}";
    }

    private Object getValue() {
        switch (type) {
            case TITLE: return title;
            case PHRASE: return phrase;
            case ARTICLE_ID: return articleId;
            case CONCEPT_ID: return this.conceptId;
            default: throw new IllegalStateException();
        }
    }

    public Object toJson() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("type", type.toString());
        json.put(type.toString(), getValue());
        if (articleId >= 0) {
            json.put(Type.ARTICLE_ID.toString(), articleId);
        }
        if (conceptId >= 0) {
            json.put(Type.CONCEPT_ID.toString(), conceptId);
        }
        return json;
    }
}
