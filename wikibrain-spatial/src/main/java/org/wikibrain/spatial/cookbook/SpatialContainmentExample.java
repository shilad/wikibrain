package org.wikibrain.spatial.cookbook;

import com.google.common.collect.Sets;
import gnu.trove.set.TIntSet;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.constants.Layers;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialContainmentDao;
import org.wikibrain.wikidata.WikidataDao;

import java.util.Set;

/**
 * Created by bjhecht on 4/8/14.
 */
public class SpatialContainmentExample {


    public static void main(String[] args){


        try {

            Env env = new EnvBuilder().build();
            Configurator c = env.getConfigurator();

            SpatialContainmentDao scDao = c.get(SpatialContainmentDao.class);
            WikidataDao wdDao = c.get(WikidataDao.class);
            LocalPageDao lpDao = c.get(LocalPageDao.class);

            LanguageSet loadedLangs = lpDao.getLoadedLanguages();

            // set up the parameters for the call to getContainedItemIds
            String containerName = "Israel";

            Set<String> subLayers = Sets.newHashSet();
            subLayers.add("wikidata");

            LocalPage lp = lpDao.getByTitle(new Title(containerName,Language.getByLangCode("simple")), NameSpace.ARTICLE);
            Integer id = wdDao.getItemId(lp);
            TIntSet containedItemIds = scDao.getContainedItemIds(id, Layers.COUNTRY, RefSys.EARTH,
                    subLayers, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);


            int counter = 0;
            System.out.println("Items contained in the spatial footprint of the article '" + lp.getTitle() + "' are:");

            for (int cId : containedItemIds.toArray()){
                UniversalPage univPage = wdDao.getUniversalPage(cId);
                Title t = univPage.getBestEnglishTitle(lpDao, true);
                System.out.println(t.getCanonicalTitle());
                counter++;
            }

            System.out.printf("Found %d items\n", counter);




        } catch(Exception e){
            e.printStackTrace();
        }



    }

}
