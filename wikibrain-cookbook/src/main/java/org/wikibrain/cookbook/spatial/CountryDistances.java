package org.wikibrain.cookbook.spatial;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataStatement;
import org.wikibrain.wikidata.WikidataValue;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculates the distance between all countries in Wikipedia.
 *
 * @author Shilad Sen
 */
public class CountryDistances {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        WikidataDao wd = env.getConfigurator().get(WikidataDao.class);
        SpatialDataDao sd = env.getConfigurator().get(SpatialDataDao.class);

        // Get center of countries
        Map<String, Point> countryCenters = new HashMap<String, Point>();
        for (WikidataStatement st : wd.getByValue("instance of", WikidataValue.forItem(6256))) {
            int countryId = st.getItem().getId();
            WikidataEntity country = wd.getItem(countryId);
            Geometry geometry = sd.getGeometry(countryId, "wikidata", "earth");
            if (geometry != null) {
                countryCenters.put(country.getLabels().get(Language.EN), geometry.getCentroid());
            }
        }

        // Estimate distance betwen them.
        FileWriter writer = new FileWriter("country_kms.txt");
        for (String country1 : countryCenters.keySet()) {
            for (String country2 : countryCenters.keySet()) {
                Point p1 = countryCenters.get(country1);
                Point p2 = countryCenters.get(country2);
                GeodeticCalculator geoCalc = new GeodeticCalculator();
                geoCalc.setStartingGeographicPoint(p1.getX(), p1.getY());
                geoCalc.setDestinationGeographicPoint(p2.getX(), p2.getY());
                double km = -1.0;
                try {
                    km = geoCalc.getOrthodromicDistance() / 1000;
                } catch (ArithmeticException e) {
                }
                writer.write(country1 + "\t" + country2 + "\t" + km + "\n");
            }
        }
        writer.close();
    }
}
