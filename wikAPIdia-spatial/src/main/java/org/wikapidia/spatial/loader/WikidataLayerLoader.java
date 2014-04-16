package org.wikapidia.spatial.loader;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.wikidata.WikidataDao;
import org.wikapidia.wikidata.WikidataFilter;
import org.wikapidia.wikidata.WikidataSqlDao;
import org.wikapidia.wikidata.WikidataStatement;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bjhecht on 4/1/14.
 */
public abstract class WikidataLayerLoader {


    protected final WikidataDao wdDao;
    protected final SpatialDataDao spatialDao;
    private final MetaInfoDao miDao;

    public static final String EARTH_REF_SYS_NAME = "earth";

    private static final Logger LOG = Logger.getLogger(WikidataLayerLoader.class.getName());


    public WikidataLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        this.wdDao = wdDao;
        this.spatialDao = spatialDao;
        this.miDao = null;
    }

    protected abstract WikidataFilter getWikidataFilter();

    public final void loadData() throws WikapidiaException {

        try {

            int matches = 0;
            int count = 0;
            LanguageSet loadedLangs = new LanguageSet("simple,lad,la");
            ((WikidataSqlDao)wdDao).setFetchSize(5);
            Iterable<WikidataStatement> statements = wdDao.get(getWikidataFilter());
            for (WikidataStatement statement : statements){

                UniversalPage uPage = wdDao.getUniversalPage(statement.getItem().getId());
                if (uPage != null && uPage.isInLanguageSet(loadedLangs, false)){
                    matches++;
                    storeStatement(statement);
                }

                count++;
                if (count % 10000 == 0){
                    LOG.log(Level.INFO, "Matched " + matches + " out of " + count + " statements from " + this.getClass().getName());
                }

            }

        }catch(DaoException e){
            throw new WikapidiaException(e);
        }

    }

    protected abstract boolean storeStatement(WikidataStatement statement) throws DaoException;


}
