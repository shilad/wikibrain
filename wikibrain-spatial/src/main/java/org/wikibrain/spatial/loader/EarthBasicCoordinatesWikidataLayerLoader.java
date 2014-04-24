package org.wikibrain.spatial.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
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

    public EarthBasicCoordinatesWikidataLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        super(wdDao, spatialDao);
    }

    protected static Geometry jsonToGeometry(JsonObject json){

        try {

            Double latitude = json.get("latitude").getAsDouble();
            Double longitude = json.get("longitude").getAsDouble();
            String globe = null;
            try{
                globe = json.get("globe").getAsString();
            }
            catch(Exception e){
                //do nothing....default for "null" globe is earth
            }


            if (globe != null && !(globe.endsWith(EARTH_ITEM_ID) || globe.endsWith("earth"))) {
                LOG.log(Level.INFO, "Found non-Earth coordinate location: " + json);
                return null; // check to make sure these refer to the Earth
            }

            Coordinate[] coords = new Coordinate[1];
            coords[0] = new Coordinate(longitude, latitude);
            CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
            Point p = new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326));


            return p;

        }catch(Exception e){
            LOG.log(Level.WARNING, "Parse error while reading Wikidata json value: " + json + " (" + e.getMessage() + ")");
            return null;
        }

    }


    @Override
    protected WikidataFilter getWikidataFilter() {

        LOG.log(Level.INFO, "Searching for Wikidata statements with property ID (coordinate locations): " + COORDINATE_LOCATION_PROPERTY_ID);
        return (new WikidataFilter.Builder()).withPropertyId(COORDINATE_LOCATION_PROPERTY_ID).build();
    }

    @Override
    protected boolean storeStatement(WikidataStatement statement) throws DaoException {


        int itemId = statement.getItem().getId();
        Geometry g = jsonToGeometry(statement.getValue().getJsonValue().getAsJsonObject());
        if (g != null && spatialDao.getGeometry(itemId, LAYER_NAME, EARTH_REF_SYS_NAME) == null) {
            spatialDao.saveGeometry(itemId, LAYER_NAME, EARTH_REF_SYS_NAME,  g);
            return true;
        }else{
            return false;
        }

    }
}
