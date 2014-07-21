package org.wikibrain.spatial.loader;

import com.vividsolutions.jts.geom.*;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by bjhecht on 4/1/14.
 */
public class EarthBasicCoordinatesWikidataLayerLoader extends WikidataLayerLoader {

    protected static final int COORDINATE_LOCATION_PROPERTY_ID = 625;
    private static final String EARTH_ITEM_ID = "Q2";
    private static final String LAYER_NAME = "wikidata";

    private static final Logger LOG = Logger.getLogger(EarthBasicCoordinatesWikidataLayerLoader.class.getName());

    private static final Pattern p = Pattern.compile("longitude=(.+?), latitude=(.+?), globe=(.+?)");
    private final MetaInfoDao metaDao;

    public EarthBasicCoordinatesWikidataLayerLoader(MetaInfoDao metaDao, WikidataDao wdDao, SpatialDataDao spatialDao) {
        super(wdDao, spatialDao);
        this.metaDao = metaDao;
    }


    @Override
    protected WikidataFilter getWikidataFilter() {

        LOG.log(Level.INFO, "Searching for Wikidata statements with property ID (coordinate locations): " + COORDINATE_LOCATION_PROPERTY_ID);
        return (new WikidataFilter.Builder()).withPropertyId(COORDINATE_LOCATION_PROPERTY_ID).build();
    }

    @Override
    protected boolean storeStatement(WikidataStatement statement) throws DaoException {


        int itemId = statement.getItem().getId();
        Geometry g = WikiBrainSpatialUtils.jsonToGeometry(statement.getValue().getJsonValue().getAsJsonObject());
        if (g != null && spatialDao.getGeometry(itemId, LAYER_NAME, EARTH_REF_SYS_NAME) == null) {
            spatialDao.saveGeometry(itemId, LAYER_NAME, EARTH_REF_SYS_NAME,  g);
            metaDao.incrementRecords(Geometry.class);
            return true;
        }else{
            return false;
        }

    }
}
