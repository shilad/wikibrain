package org.wikibrain.spatial.loader;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.Map;

/**
* Created by bjhecht on 4/1/14.
*/
public class EarthInstanceOfCoordinatesLayerLoader extends EarthBasicCoordinatesWikidataLayerLoader{

    protected static final int INSTANCE_OF_PROPERTY_ID = 31;
    private Map<Integer, String> typeNameCache = Maps.newHashMap();

    public EarthInstanceOfCoordinatesLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        super(wdDao, spatialDao);
    }

    @Override
    protected boolean storeStatement(WikidataStatement statement) throws DaoException {

        int itemId = statement.getItem().getId();
        Geometry g = EarthBasicCoordinatesWikidataLayerLoader.jsonToGeometry(statement.getValue().getJsonValue().getAsJsonObject());
        if (g != null) {
            Iterable<WikidataStatement> instanceOfStatements = wdDao.get((new WikidataFilter.Builder()).withEntityId(itemId).withPropertyId(INSTANCE_OF_PROPERTY_ID).build());
            int count = 0;
            for (WikidataStatement instanceOfStatement : instanceOfStatements) {
                int typeItemId = instanceOfStatement.getValue().getItemValue();
                String layerName = "wikidata_"+Integer.toString(typeItemId);
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
