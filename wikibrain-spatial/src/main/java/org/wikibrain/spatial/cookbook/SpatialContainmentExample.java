package org.wikibrain.spatial.cookbook;

import com.google.common.collect.Sets;
import gnu.trove.set.TIntSet;
//import org.joda.time.DateTime;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.Pageview;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.*;
import org.wikibrain.spatial.constants.Layers;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialContainmentDao;
import org.wikibrain.wikidata.WikidataDao;
//import org.wikibrain.pageview.PageViewDao;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by bjhecht on 4/8/14.
 */
public class SpatialContainmentExample {

    static int getLength(Iterable<LocalLink> x){
        Iterator<LocalLink> i = x.iterator();
        int counter = 0;
        while (i.hasNext()) {
            i.next();
            counter++;
        }
        return counter;
    }

    public static void main(String[] args){


        try {

            Env env = new EnvBuilder().build();
            Configurator c = env.getConfigurator();

            SpatialContainmentDao scDao = c.get(SpatialContainmentDao.class);
            LocalLinkDao llDao = c.get(LocalLinkDao.class);
            //WikidataDao wdDao = c.get(WikidataDao.class);
            LocalPageDao lpDao = c.get(LocalPageDao.class);
            UniversalPageDao upDao = c.get(UniversalPageDao.class);
            //PageViewDao pvDao = c.get(PageViewDao.class);
            LanguageSet loadedLangs = lpDao.getLoadedLanguages();

            // set up the parameters for the call to getContainedItemIds
            /*
            String containerName = "Israel";
              LocalPage lp = lpDao.getByTitle(new Title(containerName,Language.getByLangCode("en")), NameSpace.ARTICLE);
*/
            Set<String> subLayers = Sets.newHashSet();
            subLayers.add("wikidata");



            Integer id = 152;

            TIntSet containedItemIds = scDao.getContainedItemIds(id, "CN_Prefecture", RefSys.EARTH,
                    subLayers, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);


            int counter = 0;
            //System.out.println("Items contained in the spatial footprint of the article '" + lp.getTitle() + "' are:");
            Language lang = Language.ZH;
            for (int cId : containedItemIds.toArray()){
                UniversalPage univPage = upDao.getById(cId);
                if(counter % 1000 == 0)
                System.out.printf("Got %d out of %d\n", counter, containedItemIds.size());


                Title t = univPage.getBestEnglishTitle(lpDao, true);
                //if(getLength(llDao.getLinks(lang, univPage.getLocalId(lang), true)) > 2000)
                Integer pageView = 0;
                 //Integer pageView  =pvDao.getNumViews(lang, upDao.getById(cId).getLocalId(lang), new DateTime(2014, 8, 1, 1, 0), new DateTime(2014, 8, 31, 23, 0));
                 System.out.println(String.valueOf(cId) + " " + t.getCanonicalTitle() + " " + getLength(llDao.getLinks(lang, univPage.getLocalId(lang), false)) + " " + getLength(llDao.getLinks(lang, univPage.getLocalId(lang), true)) + " " + pageView);
                counter++;
            }

            System.out.printf("Found %d items\n", counter);




        } catch(Exception e){
            e.printStackTrace();
        }



    }

}
