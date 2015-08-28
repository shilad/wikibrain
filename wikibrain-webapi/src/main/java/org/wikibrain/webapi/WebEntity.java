package org.wikibrain.webapi;

import org.apache.commons.lang3.text.WordUtils;
import org.wikibrain.core.lang.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        public String toPluralString() {
            return toString() + "s";
        }
    };

    private Type type;
    private Language lang;
    private String title;
    private String phrase;
    private int articleId = -1;
    private int conceptId = -1;

    public static WebEntity titleEntity(Language lang, String title) {
        WebEntity w = new WebEntity();
        w.type = Type.TITLE;
        w.lang = lang;
        w.title = title;
        return w;
    }

    public static WebEntity phraseEntity(Language lang, String phrase) {
        WebEntity w = new WebEntity();
        w.type = Type.PHRASE;
        w.lang = lang;
        w.phrase = phrase;
        return w;
    }

    public static WebEntity articleEntity(Language lang, int articleId) {
        WebEntity w = new WebEntity();
        w.type = Type.ARTICLE_ID;
        w.lang = lang;
        w.articleId = articleId;
        return w;
    }

    public static WebEntity conceptEntity(Language lang, int conceptId) {
        WebEntity w = new WebEntity();
        w.type = Type.CONCEPT_ID;
        w.lang = lang;
        w.conceptId = conceptId;
        return w;
    }

    private WebEntity() {}

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

    public void setTitle(String title) {
        this.title = title;
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
