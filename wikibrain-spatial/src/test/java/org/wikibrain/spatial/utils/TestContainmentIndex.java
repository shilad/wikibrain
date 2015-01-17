package org.wikibrain.spatial.utils;


import com.vividsolutions.jts.geom.*;
import org.apache.commons.io.FileUtils;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.WikiBrainShapeFile;
import org.wikibrain.spatial.util.ContainmentIndex;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class TestContainmentIndex {
    private static final String SHP_PREFIX = "cb_2013_us_state_20m";
    private Random random = new Random();
    private GeometryFactory factory = new GeometryFactory(new PrecisionModel(),8307);
    private File shpDir;
    private List<StateGeom> states;
    private ContainmentIndex index;

    private

    static final class StateGeom {
        public String name;
        public int id;
        public Geometry geometry;
    }


    @Before
    public void prepareIndex() throws IOException {

        File shpDir = WpIOUtils.createTempDirectory("wikibrain-state-shp");
        for (String ext : WikiBrainShapeFile.EXTENSIONS) {
            String src = "/states/" + SHP_PREFIX + ext;
            InputStream is = WpIOUtils.class.getResourceAsStream(src);
            if (is == null) {
                throw new FileNotFoundException("Unknown resource: " + src);
            }
            File dest = FileUtils.getFile(shpDir, SHP_PREFIX + ext);
            FileUtils.copyInputStreamToFile(is, dest);
        }
        FileUtils.forceDeleteOnExit(shpDir);
        WikiBrainShapeFile shp = new WikiBrainShapeFile(FileUtils.getFile(shpDir, SHP_PREFIX + ".shp"));
        SimpleFeatureIterator iter = shp.getFeatureIter();
        states = new ArrayList<StateGeom>();

        this.index = new ContainmentIndex();
        int i = 0;
        while (iter.hasNext()) {
            SimpleFeature f = iter.next();
            StateGeom sg = new StateGeom();
            sg.name = (String) f.getAttribute("NAME");
            sg.geometry = (Geometry) f.getDefaultGeometry();
            sg.id = i++;
            states.add(sg);
            index.insert(sg.id, sg.geometry);
        }

    }

    @Test
    public void testSimple() throws DaoException {

        // In minnesota
        List<ContainmentIndex.Result> result = index.getContainer(point(-97.0448, 48.8994));
        assertEquals(1, result.size());
        assertEquals("Minnesota", name(result.get(0).id));

        // just across the border, but only near MN.
        result = index.getContainer(point(-96.9167, 49.0968));
        assertEquals(1, result.size());
        assertEquals("Minnesota", name(result.get(0).id));

        // just across the border, but equidistant to both MN and ND
        result = index.getContainer(point(-97.2286, 49.0136));
        assertEquals(2, result.size());
        assertTrue(Arrays.asList("Minnesota", "North Dakota").contains(name(result.get(0).id)));
        assertTrue(Arrays.asList("Minnesota", "North Dakota").contains(name(result.get(1).id)));

        // just across the border, but equidistant to both MN and ND
        result = index.getContainer(point(-97.2286, 49.2136));
        assertEquals(0, result.size());
    }

    private Point point(double lon, double lat) {
        return factory.createPoint(new Coordinate(lon, lat));
    }

    private String name(int id) {
        for (StateGeom g : states) {
            if (g.id == id) {
                return g.name;
            }
        }
        throw new IllegalArgumentException(""+id);
    }
}
