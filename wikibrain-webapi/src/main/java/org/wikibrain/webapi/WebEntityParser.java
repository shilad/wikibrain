package org.wikibrain.webapi;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Title;

import java.util.ArrayList;
import java.util.List;

import static org.wikibrain.webapi.WebEntity.Type.*;

/**
 * @author Shilad Sen
 */
public class WebEntityParser {
//    private final UniversalPageDao conceptDao;
    private final LocalPageDao pageDao;

    public WebEntityParser(Env env) throws ConfigurationException {
//        this.conceptDao = env.getConfigurator().get(UniversalPageDao.class);
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
    }

    public WebEntity extractEntity(WikiBrainWebRequest req) throws WikiBrainWebException, DaoException {
        int numMatches = 0;
        for (WebEntity.Type t : WebEntity.Type.values()) {
            if (req.hasParam(t.toString())) numMatches++;
        }

        if (numMatches != 1) {
            String errorMessage = "Must specify exactly one of the following params:";
            for (WebEntity.Type t : WebEntity.Type.values()) {
                errorMessage += " " + t;
            }
            throw new WikiBrainWebException(errorMessage);
        }

        Language lang = req.getLanguage();
        for (WebEntity.Type t : WebEntity.Type.values()) {
            if (req.hasParam(t.toString())) {
                String value = req.getParam(t.toString());
                return makeWebEntity(lang, t, value);
            }
        }
        throw new IllegalStateException();
    }

    public List<WebEntity> extractEntityList(WikiBrainWebRequest req) throws WikiBrainWebException, DaoException {
        int numMatches = 0;
        for (WebEntity.Type t : WebEntity.Type.values()) {
            if (req.hasParam(t.toPluralString())) numMatches++;
        }

        if (numMatches != 1) {
            String errorMessage = "Must specify exactly one of the following params:";
            for (WebEntity.Type t : WebEntity.Type.values()) {
                errorMessage += " " + t.toPluralString();
            }
            throw new WikiBrainWebException(errorMessage);
        }

        String values = null;
        WebEntity.Type type = null;
        Language lang = req.getLanguage();
        for (WebEntity.Type t : WebEntity.Type.values()) {
            if (req.hasParam(t.toPluralString())) {
                type = t;
                values = req.getParam(t.toPluralString());
                break;
            }
        }
        if (type == null) throw new IllegalStateException();

        List<WebEntity> result = new ArrayList<WebEntity>();
        for (String value : values.split("\\|")) {
            result.add(makeWebEntity(lang, type, value));
        }
        return result;
    }


    private WebEntity makeWebEntity(Language lang, WebEntity.Type t, String value) throws DaoException {
        WebEntity we;
        switch (t) {
            case TITLE:
                we = WebEntity.titleEntity(lang, value);
                Title title = new Title(we.getTitle(), we.getLang());
                we.setArticleId(pageDao.getIdByTitle(title));
                break;
            case PHRASE:
                we  = WebEntity.phraseEntity(lang, value);
                break;
            case ARTICLE_ID:
                int pageId = Integer.valueOf(value);
                we = WebEntity.articleEntity(lang, pageId);
                LocalPage page = pageDao.getById(lang, pageId);
                if (page == null) {
                    throw new WikiBrainWebException("No " + lang.getLangCode() + " article with id " +  pageId);
                }
                we.setTitle(page.getTitle().getCanonicalTitle());
                break;
            case CONCEPT_ID:
                we = WebEntity.conceptEntity(lang, Integer.valueOf(value));
                break;
            default: throw new IllegalStateException();
        }
        return we;
    }
}
