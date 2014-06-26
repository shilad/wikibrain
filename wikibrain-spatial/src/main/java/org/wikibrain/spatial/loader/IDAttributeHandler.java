package org.wikibrain.spatial.loader;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalArticle;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.phrases.TitleRedirectPhraseAnalyzer;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.*;
import java.util.logging.Level;

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
        if (lcAttrName.startsWith("title")) {
            return new TitleAttributeHandler(name, analyzer, wdDao);
        }else if (lcAttrName.startsWith("descrip")){
            return null;
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

            // set language to simple instead of english
            //TODO: Aw...the hack!
            if (parts[1].toLowerCase().equals("en")) parts[1] = "simple"; // TEMPORARY HACK

            myLang = Language.getByLangCode(parts[1]);

            this.analyzer = analyzer;

        }

        @Override
        public Integer getWikidataItemIdForId(Object id) throws WikiBrainException{
            try {

                System.out.println(id);

                LinkedHashMap<LocalId, Float> candidate = analyzer.resolve(myLang, (String)id, 1);
                if (candidate.size() == 0) return null;
                LocalId li = candidate.keySet().iterator().next();
                return wdDao.getItemId(li);

            }catch(DaoException e){

                throw new WikiBrainException(e);

            }
        }

        public Integer getWikidataItemIdForId(Object id, String description, LocalPageDao localPageDao, UniversalPageDao uDao) throws WikiBrainException {
            try {

                final LinkedHashMap<LocalId, Float> candidates = analyzer.resolve(myLang, (String) id, 5);
                if (candidates == null || candidates.size() == 0) {
                    System.out.println(id+" has no match");
                    return null;
                }

                List<LocalId> keyList = new ArrayList<LocalId>();
                keyList.addAll(candidates.keySet());

                // sort high to low
                Collections.sort(keyList, new Comparator<LocalId>() {
                    @Override
                    public int compare(LocalId o1, LocalId o2) {
                        if (candidates.get(o1) != null && candidates.get(o2) != null) {
                            return -Float.compare(candidates.get(o1), candidates.get(o2));
                        }
                        return 0;
                    }
                });
                if (keyList.size()==1){
                    System.out.println("choosing only option");
                    return wdDao.getItemId(keyList.get(0));
                }
                System.out.println(keyList.get(0)+" "+keyList.get(1));
                int conceptId1 = wdDao.getItemId(keyList.get(0));
                int conceptId2 = wdDao.getItemId(keyList.get(1));
                System.out.println((String) id+" "+conceptId1+" "+candidates.get(keyList.get(0))+ " "+ conceptId2+" "+candidates.get(keyList.get(1)));

                // iterate over keys from high to low score
                for (LocalId localId : keyList) {

                    int conceptId = wdDao.getItemId(localId);

                    // use getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
                    List<WikidataStatement> list = wdDao.getItem(conceptId).getStatements();

                    // break for loop after first proper "instance of" label found for this conceptId
                    boolean found = false;

                    // loop over this conceptId's statements
                    for (WikidataStatement st : list) {

                        //simple English misses some concept local pages
                        if (st.getProperty() != null && st.getProperty().getId() == 31) {
                            // the id belongs to the "instance of" label
                            int instanceOfId = st.getValue().getIntValue();
                            try {
                                // universal id corresponding to "instance of" label
                                UniversalPage concept2 = uDao.getById(instanceOfId, 1);
                                // local page ditto
                                LocalPage page = localPageDao.getById(myLang, concept2.getLocalId(myLang));

                                // check if there's a spatial keyword in this instance-of label title
                                if (page.getTitle().toString().toLowerCase().contains(description.toLowerCase())) {
                                    System.out.println("choosing "+conceptId);
                                    return conceptId;

                                }

                            } catch (NullPointerException e) {
                                // try to check title in alternate manner because sometimes can't find local page
                                if (wdDao.getItem(instanceOfId).getLabels().get(Language.EN) != null) {
                                    if (wdDao.getItem(instanceOfId).getLabels().get(Language.EN).toString().toLowerCase().contains(description.toLowerCase())) {
                                        System.out.println("choosing "+conceptId);

                                        return conceptId;
                                    }
                                } else if (wdDao.getItem(instanceOfId).getLabels().get(Language.SIMPLE) != null) {
                                    if (wdDao.getItem(instanceOfId).getLabels().get(Language.SIMPLE).toString().toLowerCase().contains(description.toLowerCase())) {
                                        System.out.println("choosing "+conceptId);
                                        return conceptId;
                                    }
                                }
                            }
                        }
                    }
                }
                // if didn't find keyword match, return best score
                System.out.println("choosing best score");
                return wdDao.getItemId(keyList.get(0));
            } catch (DaoException e) {
                System.out.println("throw exception in getwikidataid");
                throw new WikiBrainException(e);
            }
        }
    }


}
