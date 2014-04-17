package org.wikapidia.spatial.cookbook;

import com.google.common.collect.Sets;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.spatial.core.dao.SpatialContainmentDao;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.wikidata.WikidataDao;

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
            String containerName = "Cook County, Illinois";
            String layerName = "uscounties_lowres_beta";
            Set<String> subLayers = Sets.newHashSet();
            subLayers.add("wikidata");


            LocalPage lp = lpDao.getByTitle(new Title(containerName,Language.getByLangCode("simple")), NameSpace.ARTICLE);
            Integer id = wdDao.getItemId(lp);
            TIntSet containedItemIds = scDao.getContainedItemIds(id,layerName, "earth", subLayers, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);

            System.out.println("Items contained in the spatial footprint of the article '" + lp.getTitle() + "' are:");

            for (int cId : containedItemIds.toArray()){
                UniversalPage univPage = wdDao.getUniversalPage(cId);
                Title t = univPage.getBestEnglishTitle(lpDao, true);
                System.out.println(t.getCanonicalTitle());
            }




        } catch(Exception e){
            e.printStackTrace();;
        }



    }

}
