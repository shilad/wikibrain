package org.wikapidia.spatial.loader;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.wikidata.WikidataDao;
import org.wikapidia.wikidata.WikidataFilter;
import org.wikapidia.wikidata.WikidataStatement;

/**
 * Created by bjhecht on 4/1/14.
 */
public abstract class WikidataLayerLoader {

    protected final WikidataDao wdDao;
    protected final SpatialDataDao spatialDao;

    public static final String EARTH_REF_SYS_NAME = "earth";

    public WikidataLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        this.wdDao = wdDao;
        this.spatialDao = spatialDao;
    }

    protected abstract WikidataFilter getWikidataFilter();

    public final void loadData() throws WikapidiaException {

        try {


            Iterable<WikidataStatement> statements = wdDao.get(getWikidataFilter());
            for (WikidataStatement statement : statements){
                storeStatement(statement);
            }

        }catch(DaoException e){
            throw new WikapidiaException(e);
        }

    }

    protected abstract boolean storeStatement(WikidataStatement statement) throws DaoException;


}
