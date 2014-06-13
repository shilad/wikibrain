package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.live.LocalLinkLiveDao;

import java.io.File;

/**
 * Created by maixa001 on 6/13/14.
 */
public class ConceptPairGeneratorTest {
    public static void main(String[] args) throws Exception {
        ConceptPairGenerator pairGenerator = null;
        Env env = EnvBuilder.envFromArgs(args);
        pairGenerator = new ConceptPairGenerator(env);

        pairGenerator.loadSignificantGeometries(new File("significantGeo.txt"));

        //create a new point
        Coordinate[] coords = new Coordinate[1];
        coords[0] = new Coordinate(-93.26437,44.988113);
        CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
        Point here = new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326)); //SRID


        int[] pair = pairGenerator.getConceptPair(here );

        System.out.println( pair[0] +"  "+pair[1]);

//        pairGenerator.extractSignificantGeometries(160);
    }
}


