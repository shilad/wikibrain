package org.wikibrain.spatial.util;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.annotations.SourceType;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Created by harpa003 on 6/25/14.
 */
public class BlackListCreator { //Need to finish
    private static int WIKIDATA_CONCEPTS = 1;
    public static Env env;
    public static Configurator conf;
    public static LanguageSet langSet;
    public static SpatialDataDao sdDao;
    public static UniversalPageDao upDao;
    public static Map<Integer, Geometry> allGeometries;

    public static void main(String args[]) throws ConfigurationException, DaoException{
        env= EnvBuilder.envFromArgs(args);
        conf = env.getConfigurator();
        sdDao = conf.get(SpatialDataDao.class);
        upDao= conf.get(UniversalPageDao.class);
        allGeometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        langSet=conf.get(LanguageSet.class);

        try{
//            buildUsingLocalIdsFromAllLangs();
//            buildUsingLocalIdsFromSimpleEn();
            buildUsingUnivIds();
        } catch (Exception e){
            System.out.println("Could not create blacklist");
        }
    }

    private static void buildUsingLocalIdsFromAllLangs() throws IOException, DaoException {
        PrintWriter pw = new PrintWriter(new FileWriter(("Blacklist.txt")));

        for (Integer conceptID : allGeometries.keySet()){
            UniversalPage upage= upDao.getById(conceptID);
            for (Language lang:langSet) {
                pw.println(upage.getLocalId(lang));
            }
        }
        pw.close();
    }

    private static void buildUsingLocalIdsFromSimpleEn() throws IOException, DaoException {
        PrintWriter pw = new PrintWriter(new FileWriter(("Blacklist.txt")));

        for (Integer conceptID : allGeometries.keySet()) {
            UniversalPage upage = upDao.getById(conceptID);
            pw.println(upage.getLocalId(Language.SIMPLE));
        }
        pw.close();
    }

    private static void buildUsingUnivIds() throws IOException, DaoException {
        PrintWriter pw = new PrintWriter(new FileWriter(("Blacklist.txt")));

        for (Integer conceptID : allGeometries.keySet()){
            UniversalPage upage= upDao.getById(conceptID);
            pw.println(upage.getUnivId());
        }
        pw.close();
    }
}
