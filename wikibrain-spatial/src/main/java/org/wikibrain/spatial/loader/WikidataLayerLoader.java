package org.wikibrain.spatial.loader;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataSqlDao;
import org.wikibrain.wikidata.WikidataStatement;

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

    public final void loadData(LanguageSet langs) throws WikiBrainException {

        try {

            int matches = 0;
            int count = 0;
            ((WikidataSqlDao)wdDao).setFetchSize(5);
            Iterable<WikidataStatement> statements = wdDao.get(getWikidataFilter());
            for (WikidataStatement statement : statements){

                UniversalPage uPage = wdDao.getUniversalPage(statement.getItem().getId());
                if (uPage != null && uPage.isInLanguageSet(langs, false)){
                    matches++;
                    try {
                        storeStatement(statement);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "storage of statement failed: " + statement.toString(), e);
                    }
                }

                count++;
                if (count % 10000 == 0){
                    LOG.log(Level.INFO, "Matched " + matches + " out of " + count + " statements from " + this.getClass().getName());
                }

            }

        }catch(DaoException e){
            throw new WikiBrainException(e);
        }

    }

    protected abstract boolean storeStatement(WikidataStatement statement) throws DaoException;


}
