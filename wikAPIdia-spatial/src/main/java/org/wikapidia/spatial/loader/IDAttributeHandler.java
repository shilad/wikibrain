package org.wikapidia.spatial.loader;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.phrases.TitleRedirectPhraseAnalyzer;
import org.wikapidia.wikidata.WikidataDao;
import org.wikapidia.wikidata.WikidataEntity;

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

    public static IDAttributeHandler getHandlerByFieldName(String name, WikidataDao wdDao, PhraseAnalyzer analyzer) throws WikapidiaException{

        if (name.startsWith("Title")){
            return new TitleAttributeHandler(name, analyzer, wdDao);
        }else if(name.matches("P\\d+")){
            throw new WikapidiaException("Property-based spatiotagging is not currently supported");
        }else{
            throw new WikapidiaException("Illegal attribute name: " + name + " " +
                    "(Shapefiles to be spatiotagged must have a specific format. Please see documentation for more information)");
        }


    }

    public abstract Integer getWikidataItemIdForId(Object id) throws WikapidiaException;


    public static class TitleAttributeHandler extends IDAttributeHandler{

        private final Language myLang;
        private final PhraseAnalyzer analyzer;


        public TitleAttributeHandler(String attrName, PhraseAnalyzer analyzer, WikidataDao wdDao){

            super(wdDao);

            String[] parts = attrName.split("_");
            myLang = Language.getByLangCode(parts[1]);
            this.analyzer = analyzer;

        }

        @Override
        public Integer getWikidataItemIdForId(Object id) throws WikapidiaException{
            try {

                LinkedHashMap<LocalPage, Float> candidate = analyzer.resolve(myLang, (String)id, 1);
                if (candidate.size() == 0) return null;
                LocalPage lp = candidate.keySet().iterator().next();
                return wdDao.getItemId(lp);

            }catch(DaoException e){

                throw new WikapidiaException(e);

            }
        }
    }


}
