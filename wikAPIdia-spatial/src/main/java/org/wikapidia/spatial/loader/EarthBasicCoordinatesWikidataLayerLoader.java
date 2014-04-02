package org.wikapidia.spatial.loader;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.wikidata.WikidataDao;
import org.wikapidia.wikidata.WikidataFilter;
import org.wikapidia.wikidata.WikidataStatement;

/**
 * Created by bjhecht on 4/1/14.
 */
public class EarthBasicCoordinatesWikidataLayerLoader extends WikidataLayerLoader {

    protected static final int COORDINATE_LOCATION_PROPERTY_ID = 625;
    private static final String LAYER_NAME = "wikidata";


    public EarthBasicCoordinatesWikidataLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        super(wdDao, spatialDao);
    }


    @Override
    protected WikidataFilter getWikidataFilter() {
        return (new WikidataFilter.Builder()).withPropertyId(COORDINATE_LOCATION_PROPERTY_ID).build();
    }

    @Override
    protected boolean storeStatement(WikidataStatement statement) throws DaoException {

        int itemId = statement.getItem().getId();
        Geometry g = null;
        spatialDao.saveGeometry(itemId, LAYER_NAME, EARTH_REF_SYS_NAME, g);
        return true;

    }
}
