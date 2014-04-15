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
public class EarthInstanceOfCoordinatesLayerLoader extends EarthBasicCoordinatesWikidataLayerLoader{

    protected static final int INSTANCE_OF_PROPERTY_ID = 31;

    public EarthInstanceOfCoordinatesLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        super(wdDao, spatialDao);
    }

    @Override
    protected boolean storeStatement(WikidataStatement statement) throws DaoException {

        int itemId = statement.getItem().getId();
        Geometry g = EarthBasicCoordinatesWikidataLayerLoader.jsonToGeometry(statement.getValue().getJsonValue().getAsJsonObject());
        if (g != null) {
            Iterable<WikidataStatement> instanceOfStatements = wdDao.get((new WikidataFilter.Builder()).withPropertyId(INSTANCE_OF_PROPERTY_ID).withEntityId(itemId).build());
            int count = 0;
            for (WikidataStatement instanceOfStatement : instanceOfStatements) {
                int typeItemId = instanceOfStatement.getValue().getItemValue();
                String layerName = Integer.toString(typeItemId);
                spatialDao.saveGeometry(itemId, layerName, EARTH_REF_SYS_NAME, g);
                count++;
            }
            if (count > 0) {
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }

    }
}
