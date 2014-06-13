package org.wikibrain.cookbook.sr;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.sr.*;
import org.wikibrain.sr.utils.ExplanationFormatter;
import org.wikibrain.wikidata.WikidataDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class SimilarityExample {
    private static final boolean GET_RID_OF_SPATIAL_ON=true;  //change this line to true in order to get rid of spatial comparisons
    private static int WIKIDATA_CONCEPTS = 1;
    public static Env env;
    public static Configurator conf;
    public static LocalPageDao lpDao;
    public static Language simple;
    public static SpatialDataDao sdDao;
    public static UniversalPageDao upDao;
    public static WikidataDao wDao;
    public static Map<Integer, Geometry> allGeometries;

    public static void main(String args[]) throws ConfigurationException, DaoException {
        // Initialize the WikiBrain environment and get the local page dao
        //Env env = new EnvBuilder().build();
        env= EnvBuilder.envFromArgs(args);
        conf = env.getConfigurator();
        lpDao = conf.get(LocalPageDao.class);
        simple = Language.getByLangCode("simple");
        sdDao = conf.get(SpatialDataDao.class);
        upDao= conf.get(UniversalPageDao.class);
        wDao= conf.get(WikidataDao.class);
        allGeometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");

        // Retrieve the "ensemble" sr metric for simple english
        MonolingualSRMetric sr = conf.get(
                MonolingualSRMetric.class, "ensemble",
                "language", simple.getLangCode());

        //Similarity between strings
        String pairs[][] = new String[][] {
                { "Cat", "Kitty" },
                { "Obama", "President" },
                { "Tires", "Car" },
                { "Java", "Computer" },
                { "Dog", "Computer" },
                { "Obama", "Japan"},
                { "Dog", "New York City"},
                { "Montana", "America"}
        };

        for (String pair[] : pairs) {
            if(GET_RID_OF_SPATIAL_ON) {
                if (isSpatial(pair[0]) || isSpatial(pair[1])) {
                    System.out.println("This is a spatial comparison" + ": '" + pair[0] + "', '" + pair[1] + "'");
                } else {
                    SRResult s = sr.similarity(pair[0], pair[1], false);
                    System.out.println(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'");
                }
            } else {
                SRResult s = sr.similarity(pair[0], pair[1], false);
                System.out.println(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'");
            }
        }
    }
    public static boolean isSpatial(String s) throws DaoException{
        int localID=lpDao.getIdByTitle(s, Language.SIMPLE, NameSpace.ARTICLE);
        UniversalPage page= upDao.getByLocalPage(lpDao.getById(Language.SIMPLE, localID), WIKIDATA_CONCEPTS);
        return (allGeometries.containsKey(page.getUnivId()));
    }



}

// Java is a city so how to deal with multiple names but names are an issue anyways
