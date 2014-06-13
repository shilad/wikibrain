package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.WikidataEntityLabels;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.LocalWikidataStatement;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class InstanceOfExtractor {

    private static int WIKIDATA_CONCEPTS = 1;


    private static final Logger LOG = Logger.getLogger(InstanceOfExtractor.class.getName());




    /**
     * Load all locations from all language editions of Wikipedia to concepts
     *
     * @throws DaoException
     */

    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator c = env.getConfigurator();
        SpatialDataDao sdDao = c.get(SpatialDataDao.class);
        UniversalPageDao upDao = c.get(UniversalPageDao.class);
        LocalPageDao lDao = c.get(LocalPageDao.class);
        WikidataDao wdao = c.get(WikidataDao.class);

        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

        //create a new point
//        Coordinate[] coords = new Coordinate[1];
//        coords[0] = new Coordinate(-93.26437,44.988113);
//        CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
//        Point here = new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326)); //SRID


        HashSet<Integer> set = new HashSet<Integer>();
        HashSet<Integer> set2 = new HashSet<Integer>();
        for (Integer conceptId : geometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            LocalPage lpage = lDao.getById(Language.SIMPLE,concept.getLocalId(Language.SIMPLE));

            //getItem because getLocalStatements returns nullpointerexception and getStatements returns empty
            List<WikidataStatement> list = wdao.getItem(conceptId).getStatements();
            boolean haveFoundOne = false;
            boolean foundNullNull = false;
            int id2=0;
            for (WikidataStatement st: list){
               // if (st.getValue().getIntValue())


                //simple English misses some concept local pages
                if (st.getProperty() != null && st.getProperty().getId()==31 ){
                    System.out.println(lpage.getTitle());
                    int id = st.getValue().getIntValue();
                    set.add(id);
                    try {

                        UniversalPage concept2 = upDao.getById(id, WIKIDATA_CONCEPTS);
                        //System.out.println(concept2==null);
                        LocalPage page = lDao.getById(Language.SIMPLE, concept2.getLocalId(Language.SIMPLE));
                        System.out.println(page.getTitle());
//                        System.out.println();
//                        System.out.println(wdao.getItem(id).getLabels().get(Language.EN));
                        haveFoundOne=true;
                    } catch(Exception e){
                        System.out.println(id);
                        System.out.println(wdao.getItem(id).getLabels().get(Language.EN));
                        System.out.println(wdao.getItem(id).getLabels().get(Language.SIMPLE));

                        //System.out.println(id);
                        if (wdao.getItem(id).getLabels().get(Language.EN)==null&&wdao.getItem(id).getLabels().get(Language.SIMPLE)==null) {
//                            set2.add(id);
                            foundNullNull = true;
                            id2=id;
                        }
                    }
                }
            }

            if (!haveFoundOne && foundNullNull ){
                set2.add(id2);
            }

            System.out.println();


//            get all points within 100 km from here
//            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
//            GeodeticCalculator calc = new GeodeticCalculator();
//            calc.setStartingGeographicPoint(here.getX(),here.getY());
//            Point current = (Point) geometries.get(conceptId);
//            calc.setDestinationGeographicPoint(current.getX(),current.getY());
//
//            if (calc.getOrthodromicDistance()<100000){
//            //if (geometries.get(conceptId).isWithinDistance(here,10)) {
//                System.out.println(concept.getBestEnglishTitle(lDao, true));
//            }
        }
        System.out.println(set.size());
        System.out.println("null/null id"+set2.size());

    }



}
