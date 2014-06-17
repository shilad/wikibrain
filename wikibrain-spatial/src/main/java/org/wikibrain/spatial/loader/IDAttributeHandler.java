package org.wikibrain.spatial.loader;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalArticleDao;
import org.wikibrain.core.dao.RedirectDao;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalArticle;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Title;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.phrases.TitleRedirectPhraseAnalyzer;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;

import java.util.LinkedHashMap;

/**
 * Created by bjhecht on 4/1/14.
 *
 * Intended to support Wikidata property attributes
 */
public abstract class IDAttributeHandler {

    protected final WikidataDao wdDao;

    public IDAttributeHandler(WikidataDao wdDao) {
        this.wdDao = wdDao;
    }

    public static IDAttributeHandler getHandlerByFieldName(String name, WikidataDao wdDao, PhraseAnalyzer analyzer) throws WikiBrainException{

        String lcAttrName = name.toLowerCase();
        if (lcAttrName.startsWith("title")){
            return new TitleAttributeHandler(name, analyzer, wdDao);
        }else if(name.matches("P\\d+")){
            throw new WikiBrainException("Property-based spatiotagging is not currently supported");
        }else{
            throw new WikiBrainException("Illegal attribute name: " + name + " " +
                    "(Shapefiles to be spatiotagged must have a specific format. Please see documentation for more information)");
        }


    }

    public abstract Integer getWikidataItemIdForId(Object id) throws WikiBrainException;


    public static class TitleAttributeHandler extends IDAttributeHandler{

        private final Language myLang;
        private final PhraseAnalyzer analyzer;


        public TitleAttributeHandler(String attrName, PhraseAnalyzer analyzer, WikidataDao wdDao){

            super(wdDao);

            String[] parts = attrName.split("_");
            //TODO: Aw...the hack!
            if (parts[1].toLowerCase().equals("en")) parts[1] = "simple"; // TEMPORARY HACK

            myLang = Language.getByLangCode(parts[1]);

            this.analyzer = analyzer;

        }

        @Override
        public Integer getWikidataItemIdForId(Object id) throws WikiBrainException{
            try {

                LinkedHashMap<LocalId, Float> candidate = analyzer.resolve(myLang, (String)id, 1);
                if (candidate.size() == 0) return null;
                LocalId li = candidate.keySet().iterator().next();
                return wdDao.getItemId(li);

            }catch(DaoException e){

                throw new WikiBrainException(e);

            }
        }
    }


}
