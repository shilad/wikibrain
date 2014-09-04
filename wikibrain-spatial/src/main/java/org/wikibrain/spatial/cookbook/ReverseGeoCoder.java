package org.wikibrain.spatial.cookbook;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.*;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.core.dao.SpatialContainmentDao;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.postgis.PostGISDB;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by toby on 9/3/14.
 */
public class ReverseGeoCoder  {

    public static void main(String[] args) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        CSVReader reader = new CSVReader(new FileReader("lat_lon_codes.csv"), ',');
        CSVWriter writer = new CSVWriter(new FileWriter("new_lat_lon_codes.csv"), ',');
        SpatialContainmentDao scDao = conf.get(SpatialContainmentDao.class);
        for(String[] row : reader.readAll()){
            double lat = Double.parseDouble(row[2]);
            double lon = Double.parseDouble(row[3]);

            Geometry g = new GeometryFactory().createPoint(new Coordinate(lat, lon));


            // *** BUILD SOMEWHAT COMPLEX QUERY ***
            PostGISDB db = scDao.getDb();
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            PropertyName geomProperty = ff.property(db.getGeometryAttributeName());

            //build ref sys clause
            PropertyName refSysProperty = ff.property(db.getRefSysAttributeName());
            Filter refSysFilter = ff.equals(refSysProperty, ff.literal("earth"));

            // build layer-related clause
            PropertyName layerProperty = ff.property(db.getLayerAttributeName());
            List<Filter> layerFilters = Lists.newArrayList();
                Filter curLayerFilter = ff.equals(layerProperty, ff.literal("country"));
                layerFilters.add(curLayerFilter);

            Filter layerFilter = ff.and(layerFilters);

            Filter geomFilter = null;
            geomFilter = ff.intersects(ff.literal(g), geomProperty);


            List<Filter> filters = Lists.newArrayList();
            filters.add(refSysFilter);
            filters.add(layerFilter);
            filters.add(geomFilter);

            Filter finalFilter = ff.and(filters);

            // *** EXECUTE QUERY ***
            FeatureSource featureSource;
            FeatureCollection containedFeatures;
            try {

                featureSource = db.getFeatureSource();
                containedFeatures = featureSource.getFeatures(finalFilter);
            }
            catch (IOException e){
                throw new DaoException();
            }
            FeatureIterator featureIterator = containedFeatures.features();

            TIntSet rVal = new TIntHashSet();

            while (featureIterator.hasNext()){

                Feature f = featureIterator.next();
                Integer itemId = (Integer)f.getProperty(db.getItemIdAttributeName()).getValue();
                rVal.add(itemId);

            }
            featureIterator.close();









        }










    }

}
