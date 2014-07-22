package org.wikibrain.spatial.util;

import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Created by bjhecht on 5/21/14.
*/
public class WikiBrainSpatialUtils {

    private static final String EARTH_ITEM_ID = "Q2";

    private static final Logger LOG = Logger.getLogger(WikiBrainSpatialUtils.class.getName());


    public static Geometry jsonToGeometry(JsonObject json){

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
//                LOG.log(Level.INFO, "Found non-Earth coordinate location: " + json);
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
}
