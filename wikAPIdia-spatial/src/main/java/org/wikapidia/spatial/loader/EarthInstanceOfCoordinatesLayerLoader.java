package org.wikapidia.spatial.loader;

import org.postgis.Geometry;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.wikidata.WikidataDao;
import org.wikapidia.wikidata.WikidataFilter;
import org.wikapidia.wikidata.WikidataStatement;

/**
 * Created by bjhecht on 4/1/14.
 */
public class EarthInstanceOfCoordinatesLayerLoader extends EarthBasicCoordinatesWikidataLayerLoader{

    protected static final int INSTANCE_OF_PROPERTY_ID = 31;

    public EarthInstanceOfCoordinatesLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        super(wdDao, spatialDao);
    }

    @Override
    protected boolean storeStatement(WikidataStatement statement) throws DaoException {

        int itemId = statement.getItem().getId();
        Iterable<WikidataStatement> instanceOfStatements = wdDao.get((new WikidataFilter.Builder()).withPropertyId(INSTANCE_OF_PROPERTY_ID).withEntityId(itemId).build());
        for (WikidataStatement instanceOfStatement : instanceOfStatements){
            int typeItemId = instanceOfStatement.getValue().getItemValue();
            String layerName = Integer.toString(typeItemId);
            Geometry g = null;
//            spatialDao.saveGeometry(itemId, layerName, EARTH_REF_SYS_NAME, g);
        }
        return false;

    }
}
