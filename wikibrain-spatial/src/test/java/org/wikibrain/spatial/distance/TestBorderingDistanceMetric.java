package org.wikibrain.spatial.distance;


import com.vividsolutions.jts.geom.*;
import org.apache.commons.io.FileUtils;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.WikiBrainShapeFile;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.utils.Scoreboard;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Shilad Sen
 */
public class TestBorderingDistanceMetric {
    private static final String SHP_PREFIX = "cb_2013_us_state_20m";
    private Random random = new Random();
    private GeometryFactory factory = new GeometryFactory(new PrecisionModel(),8307);
    private File shpDir;
    private List<StateGeom> states;
    private BorderingDistanceMetric metric;

    static final class StateGeom {
        public String name;
        public int id;
        public Geometry geometry;
    }

    @Before
    public void prepareMetric() throws IOException, DaoException {
        shpDir = WpIOUtils.createTempDirectory("wikibrain-state-shp");
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

        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        int i = 0;
        while (iter.hasNext()) {
            SimpleFeature f = iter.next();
            StateGeom sg = new StateGeom();
            sg.name = (String) f.getAttribute("NAME");
            sg.geometry = (Geometry) f.getDefaultGeometry();
            sg.id = i++;
            points.put(sg.id, sg.geometry);
            states.add(sg);
        }

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("states", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);
        metric = new BorderingDistanceMetric(dao, "states");
        metric.enableCache(true);
    }

    @After
    public void cleanupDao() {
        FileUtils.deleteQuietly(shpDir);
    }

    @Test
    public void testSimple() throws DaoException {
        assertEquals(0, metric.distance(g("Minnesota"), g("Minnesota")), 0.01);
        assertEquals(1, metric.distance(g("Wisconsin"), g("Minnesota")), 0.01);
        assertEquals(1, metric.distance(g("North Dakota"), g("Minnesota")), 0.01);
        assertEquals(1, metric.distance(g("South Dakota"), g("Minnesota")), 0.01);
        assertEquals(1, metric.distance(g("Iowa"), g("Minnesota")), 0.01);
        assertEquals(2, metric.distance(g("Illinois"), g("Minnesota")), 0.01);
        assertEquals(4, metric.distance(g("Texas"), g("Minnesota")), 0.01); // MN -> IA -> MO -> MS -> TX

        // Test the point variant
        assertEquals(0, metric.distance(g("Minnesota").getCentroid(), g("Minnesota")), 0.01);
        assertEquals(0, metric.distance(g("Minnesota"), g("Minnesota").getCentroid()), 0.01);
        assertEquals(4, metric.distance(g("Minnesota").getCentroid(), g("Texas").getCentroid()), 0.01);
    }


    @Test
    public void testForceContains() throws DaoException {
        metric.setForceContains(true);

        // Test polygons
        assertEquals(0, metric.distance(g("Minnesota"), g("Minnesota")), 0.01);
        assertEquals(1, metric.distance(g("North Dakota"), g("Minnesota")), 0.01);

        // Test the point variant
        assertEquals(0, metric.distance(g("Minnesota").getCentroid(), g("Minnesota")), 0.01);
        assertEquals(1, metric.distance(g("North Dakota").getCentroid(), g("Minnesota")), 0.01);

        // Points nearby states should behave identically to the nearby states.
        Point aboveMn = WikiBrainSpatialUtils.getPoint(
                g("Minnesota").getEnvelopeInternal().getMaxY() + 2,
                g("Minnesota").getCentroid().getX()
        );
        assertEquals(0, metric.distance(aboveMn, g("Minnesota")), 0.01);
        assertEquals(1, metric.distance(aboveMn, g("North Dakota")), 0.01);
    }

    @Test
    public void testKnn() throws DaoException {
        List<SpatialDistanceMetric.Neighbor> neighbors = metric.getNeighbors(g("Minnesota"), 100);
        for (int i = 0; i < neighbors.size(); i++) {
            SpatialDistanceMetric.Neighbor n = neighbors.get(i);
            String name = n(n.conceptId);
            if (i == 0) {
                assertEquals(name, "Minnesota");
            } else if (i <= 4) {
                assertTrue(Arrays.asList("North Dakota", "Iowa", "Wisconsin", "South Dakota").contains(name));
            }
        }
        SpatialDistanceMetric.Neighbor last =  neighbors.get(neighbors.size() - 1);
        assertEquals(8.0, last.distance, 0.01);
        assertEquals("Maine", n(last.conceptId));
    }

    @Test
    public void testMatrix() throws DaoException {
    }

    private String n(int id) {
        for (StateGeom sg : states) {
            if (sg.id == id) {
                return sg.name;
            }
        }
        throw new IllegalArgumentException("Unknown id: " + id);
    }

    private Geometry g(String name) {
        for (StateGeom sg : states) {
            if (sg.name.equalsIgnoreCase(name)) {
                return sg.geometry;
            }
        }
        throw new IllegalArgumentException(name);
    }
}
