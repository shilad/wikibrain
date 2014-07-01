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
import org.wikibrain.core.model.*;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.sr.*;
import org.wikibrain.sr.utils.ExplanationFormatter;
import org.wikibrain.wikidata.WikidataDao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class SimilarityExample {
    private static int WIKIDATA_CONCEPTS = 1;
    public static Env env;
    public static Configurator conf;
    public static LocalPageDao lpDao;
    public static Language simple;
    public static SpatialDataDao sdDao;
    public static UniversalPageDao upDao;
    public static WikidataDao wDao;
    public static Map<Integer, Geometry> allGeometries;
    public static ExplanationFormatter formatter;

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
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
        formatter= new ExplanationFormatter(lpDao);
        LocalIDToUniversalID.init(conf);

        String srMetric= "ESA"; // Retrieve the indicated sr metric for simple english

        MonolingualSRMetric sr = conf.get(
                MonolingualSRMetric.class, srMetric,
                "language", simple.getLangCode());

        //Similarity between strings
        String pairs[][] = new String[][] {
                { "Kitty", "Cat" },
                { "obama", "president" },
                { "tires", "car" },
                { "Java", "Computer" },
                { "Dog", "Computer" },
                { "Barack Obama", "Japan"},
                { "Obama", "Japan"},
                { "Dog", "New York City"},
                { "Montana", "United States of America"},
                { "Statue of Liberty", "New York City" }
        };


        for (String pair[] : pairs) {
                SRResult s = sr.similarity(pair[0], pair[1], true);
                System.out.println(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'");
                for (Explanation e:s.getExplanations()){
                    System.out.println(formatter.formatExplanation(e));
                }
            }

    }



    public static boolean isSpatial(String s) throws DaoException{
        int localID=lpDao.getIdByTitle(s, Language.SIMPLE, NameSpace.ARTICLE);
        UniversalPage page= upDao.getByLocalPage(lpDao.getById(Language.SIMPLE, localID), WIKIDATA_CONCEPTS);
        System.out.println("uID "+page.getUnivId()+" localID "+localID);
        return (allGeometries.containsKey(page.getUnivId()));
    }



}

