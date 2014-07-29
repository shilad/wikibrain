package org.wikibrain.cookbook.spatial;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.set.TIntSet;
import org.jooq.util.derby.sys.Sys;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.LocalLink;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.cookbook.tflevaluate.DistanceMetrics;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.spatial.core.dao.SpatialContainmentDao;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;
import org.wikibrain.sr.MonolingualSRMetric;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by toby on 7/10/14.
 */
public class SelfFocusBiasEvaluator {

    private static int WIKIDATA_CONCEPTS = 1;


    private static final Logger LOG = Logger.getLogger(SelfFocusBiasEvaluator.class.getName());

    private Random random = new Random();

    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final SpatialContainmentDao scDao;
    private final LocalLinkDao llDao;
    private Language lang;
    private final Map<Integer, List<Integer>> polygonWAG = new HashMap<Integer, List<Integer>>();
    private final Env env;
    private CSVWriter output;
    private String pointLayer = "wikidata";
    private String polygonLayer;
    Map<Integer, Geometry> polygons;
    Map<Integer, TIntSet> polygonContainedId = new HashMap<Integer, TIntSet>();

    public SelfFocusBiasEvaluator(Env env,  String polygonLayer) throws ConfigurationException {
        this.env = env;
        Configurator c = env.getConfigurator();
        this.sdDao = c.get(SpatialDataDao.class);
        this.lpDao = c.get(LocalPageDao.class);
        this.upDao = c.get(UniversalPageDao.class);
        this.scDao = c.get(SpatialContainmentDao.class);
        this.llDao = c.get(LocalLinkDao.class);
        this.polygonLayer = polygonLayer;
    }

    public Map<Integer, Integer> evaluate(Language lang) throws DaoException, IOException{
        this.lang = lang;
        String mostInlinkPage = "";
        String mostInlinkLang = "";
        int maxInlink = 0;

        this.polygons = sdDao.getAllGeometriesInLayer(polygonLayer, "earth");
        LOG.info(String.format("Finish loading %d polygons", polygons.size()));
        Set<String> layerSet = new HashSet<String>();
        layerSet.add(pointLayer);
        Map<Integer, Integer> polygonInlinkMap = new HashMap<Integer, Integer>();
        int polygonCounter = 0;
        TIntSet containedIds;
        for(Map.Entry<Integer, Geometry> entry : polygons.entrySet()){
            if(polygonContainedId.containsKey(entry.getKey())){
                containedIds = polygonContainedId.get(entry.getKey());
            }
            else{
                containedIds = scDao.getContainedItemIds(entry.getValue(), "earth", layerSet, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);
                polygonContainedId.put(entry.getKey(), containedIds);
            }
            int pointCounter = 0;
            LOG.info(String.format("Got %d points in polygon %d", containedIds.size(), polygonCounter));
            for(Integer c : containedIds.toArray()){
                try {
                    Iterable<org.wikibrain.core.model.LocalLink> links = llDao.getLinks(lang, upDao.getById(c).getLocalId(lang), false);
                    int counter = 0;
                    Iterator i = links.iterator();
                    while (i.hasNext()){
                        counter ++;
                        i.next();
                    }
                    if(counter > maxInlink ){
                        maxInlink = counter;
                        mostInlinkLang = lang.getEnLangName();
                        mostInlinkPage = upDao.getById(c).getBestEnglishTitle(lpDao, true).getCanonicalTitle();
                    }
                    if(polygonInlinkMap.containsKey(entry.getKey())){
                        polygonInlinkMap.put(entry.getKey(), polygonInlinkMap.get(entry.getKey()) + counter);
                    }
                    else{
                        polygonInlinkMap.put(entry.getKey(),  counter);

                    }
                    pointCounter ++;
                    if(pointCounter % 100 == 0)
                        System.out.printf("finished processing point %d out of %d \n", pointCounter, containedIds.size());
                }
                catch (Exception e){
                    // do nothing
                }

            }
            polygonCounter ++;
            System.out.printf("finished processing polygon %d out of %d \n", polygonCounter, polygons.size());

        }
        System.out.printf("Most linked page is %s from %s with %d in links\n", mostInlinkPage, mostInlinkLang, maxInlink);
        return  polygonInlinkMap;

    }
    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        SelfFocusBiasEvaluator evaluator = new SelfFocusBiasEvaluator(env, "country");
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);


        Set<Language> langSet = new HashSet<Language>();
        langSet.add(Language.getByLangCode("simple"));
        for(Language lang: langSet){
            LOG.info(String.format("Start evaluating %s", lang.getEnLangName()));
            Map<Integer, Integer> polygonInlinkMap = evaluator.evaluate(lang);
            List<Map.Entry<Integer, Integer>> inlinkList = new ArrayList<Map.Entry<Integer, Integer>>();
            inlinkList.addAll(polygonInlinkMap.entrySet());
            Collections.sort(inlinkList, new Comparator<Map.Entry<Integer, Integer>>() {
                @Override
                public int compare(Map.Entry<Integer, Integer> integerIntegerEntry, Map.Entry<Integer, Integer> integerIntegerEntry2) {
                    return integerIntegerEntry2.getValue() - integerIntegerEntry.getValue();
                }
            });
            CSVWriter output = new CSVWriter(new FileWriter("SelfFocusBias_" + lang.getLangCode() + ".csv"), ',');
            String[] entries = new String[2];
            for(Map.Entry<Integer, Integer> entry : inlinkList){
                entries[0] = upDao.getById(entry.getKey()).getBestEnglishTitle(lpDao, true).getCanonicalTitle();
                entries[1] = entry.getValue().toString();
                output.writeNext(entries);
            }
            output.flush();
            LOG.info(String.format("Finish evaluating %s", lang.getEnLangName()));

        }



    }








}
