package org.wikibrain.cookbook.core;

import au.com.bytecode.opencsv.CSVReader;
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
import org.wikibrain.core.dao.*;
import org.wikibrain.core.jooq.tables.LocalLink;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.cookbook.tflevaluate.DistanceMetrics;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.spatial.core.dao.SpatialContainmentDao;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by toby on 7/10/14.
 */


public class SelfFocusBiasEvaluator {

    public Set<Integer> GADM01Concepts = new HashSet<Integer>();

    private static class retVal {
        public Map<Integer, Integer> polygonInlinkMap = new HashMap<Integer, Integer>();
        public Map<Integer, Integer> polygonOutlinkMap = new HashMap<Integer, Integer>();
        public Map<Integer, Integer> polygonInlinkNSMap = new HashMap<Integer, Integer>();
        public Map<Integer, Integer> polygonOutlinkNSMap = new HashMap<Integer, Integer>();
        public Map<Integer, Integer> polygonPageSumMap = new HashMap<Integer, Integer>();
        public Map<Integer, Integer> polygonCategorySumMap = new HashMap<Integer, Integer>();
        public Map<Integer, Integer> polygonArticleLengthMap = new HashMap<Integer, Integer>();


    }

    private static int WIKIDATA_CONCEPTS = 1;


    private static final Logger LOG = Logger.getLogger(SelfFocusBiasEvaluator.class.getName());


    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final SpatialContainmentDao scDao;
    private final LocalLinkDao llDao;
    private final LocalCategoryMemberDao lcDao;
    private final RawPageDao rpDao;
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
        this.lcDao = c.get(LocalCategoryMemberDao.class);
        this.rpDao = c.get(RawPageDao.class);
        this.polygonLayer = polygonLayer;
    }


    public retVal evaluate(Language lang) throws DaoException, FileNotFoundException, IOException{

        CSVReader reader = new CSVReader(new FileReader("gadm_matched.csv"), ',');
        List<String[]> gadmList = reader.readAll();
        for(String[] gadmItem : gadmList){
            GADM01Concepts.add(Integer.parseInt(gadmItem[0]));
        }


        Set<Integer> spatialPages = sdDao.getAllGeometriesInLayer("wikidata").keySet();

        this.lang = lang;
        this.polygons = sdDao.getAllGeometriesInLayer(polygonLayer, "earth");
        LOG.info(String.format("Finish loading %d polygons", polygons.size()));
        Set<String> layerSet = new HashSet<String>();
        layerSet.add(pointLayer);
        Map<Integer, Integer> polygonInlinkMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> polygonOutlinkMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> polygonInlinkNSMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> polygonOutlinkNSMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> polygonPageSumMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> polygonCategorySumMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> polygonArticleLengthMap = new HashMap<Integer, Integer>();

        retVal returnValue = new retVal();

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
                //Skip GADM0/1
                if(GADM01Concepts.contains(c))
                    continue;
                try {

                    Iterable<org.wikibrain.core.model.LocalLink> inlinks = llDao.getLinks(lang, upDao.getById(c).getLocalId(lang), false);
                    Iterable<org.wikibrain.core.model.LocalLink> outlinks = llDao.getLinks(lang, upDao.getById(c).getLocalId(lang), true);
                    int inlinkCounter = 0;
                    int outlinkCounter = 0;
                    int inlinkNSCounter = 0;
                    int outlinkNSCounter = 0;
                    int categoryCounter = 0;
                    int articleLength = 0;
                    Iterator<org.wikibrain.core.model.LocalLink> i = inlinks.iterator();
                    while (i.hasNext()){
                        inlinkCounter ++;
                        org.wikibrain.core.model.LocalLink link = i.next();
                        if(!spatialPages.contains(upDao.getUnivPageId(lang, link.getSourceId())))
                            inlinkNSCounter++;
                    }

                    i = outlinks.iterator();
                    while (i.hasNext()){
                        outlinkCounter ++;
                        org.wikibrain.core.model.LocalLink link = i.next();
                        if(!spatialPages.contains(upDao.getUnivPageId(lang, link.getDestId())))
                            outlinkNSCounter++;

                    }

                    categoryCounter = lcDao.getCategoryIds(lang, upDao.getById(c).getLocalId(lang)).size();
                    articleLength = rpDao.getBody(lang, upDao.getById(c).getLocalId(lang)).length();

                    if(polygonInlinkMap.containsKey(entry.getKey())){
                        polygonInlinkMap.put(entry.getKey(), polygonInlinkMap.get(entry.getKey()) + inlinkCounter);
                    }
                    else{
                        polygonInlinkMap.put(entry.getKey(),  inlinkCounter);
                    }


                    if(polygonInlinkNSMap.containsKey(entry.getKey())){
                        polygonInlinkNSMap.put(entry.getKey(), polygonInlinkNSMap.get(entry.getKey()) + inlinkNSCounter);
                    }
                    else{
                        polygonInlinkNSMap.put(entry.getKey(),  inlinkNSCounter);
                    }


                    if(polygonOutlinkMap.containsKey(entry.getKey())){
                        polygonOutlinkMap.put(entry.getKey(), polygonOutlinkMap.get(entry.getKey()) + outlinkCounter);
                    }
                    else{
                        polygonOutlinkMap.put(entry.getKey(),  outlinkCounter);
                    }


                    if(polygonOutlinkNSMap.containsKey(entry.getKey())){
                        polygonOutlinkNSMap.put(entry.getKey(), polygonOutlinkNSMap.get(entry.getKey()) + outlinkNSCounter);
                    }
                    else{
                        polygonOutlinkNSMap.put(entry.getKey(),  outlinkNSCounter);
                    }


                    if(polygonPageSumMap.containsKey(entry.getKey())){
                        polygonPageSumMap.put(entry.getKey(), polygonPageSumMap.get(entry.getKey()) + 1);
                    }
                    else{
                        polygonPageSumMap.put(entry.getKey(),  1);
                    }


                    if(polygonCategorySumMap.containsKey(entry.getKey())){
                        polygonCategorySumMap.put(entry.getKey(), polygonCategorySumMap.get(entry.getKey()) + categoryCounter);
                    }
                    else{
                        polygonCategorySumMap.put(entry.getKey(),  categoryCounter);
                    }


                    if(polygonArticleLengthMap.containsKey(entry.getKey())){
                        polygonArticleLengthMap.put(entry.getKey(), polygonArticleLengthMap.get(entry.getKey()) + articleLength);
                    }
                    else{
                        polygonArticleLengthMap.put(entry.getKey(),  articleLength);
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

        returnValue.polygonArticleLengthMap = polygonArticleLengthMap;
        returnValue.polygonCategorySumMap = polygonCategorySumMap;
        returnValue.polygonInlinkMap = polygonInlinkMap;
        returnValue.polygonInlinkNSMap = polygonInlinkNSMap;
        returnValue.polygonOutlinkMap = polygonOutlinkMap;
        returnValue.polygonOutlinkNSMap = polygonOutlinkNSMap;
        returnValue.polygonPageSumMap = polygonPageSumMap;

        return  returnValue;


    }
    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        SelfFocusBiasEvaluator evaluator = new SelfFocusBiasEvaluator(env, "states");
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        retVal returnValue = evaluator.evaluate(Language.getByLangCode("simple"));
        CSVWriter writer = new CSVWriter(new FileWriter("empty.csv"), ',');
        String[] row = new String[9];
        row[0] = "Universal ID";
        row[1] = "Name";
        row[2] = "Inlink";
        row[3] = "Outlink";
        row[4] = "Inlink_NS";
        row[5] = "Outlink_NS";
        row[6] = "Category";
        row[7] = "Length";
        row[8] = "PageCount";

        writer.writeNext(row);
        writer.flush();

        for(Integer id : returnValue.polygonPageSumMap.keySet()){
            row[0] = id.toString();
            row[1] = upDao.getById(id).getBestEnglishTitle(lpDao, true).getCanonicalTitle();
            row[2] = returnValue.polygonInlinkMap.get(id).toString();
            row[3] = returnValue.polygonOutlinkMap.get(id).toString();
            row[4] = returnValue.polygonInlinkNSMap.get(id).toString();
            row[5] = returnValue.polygonOutlinkNSMap.get(id).toString();
            row[6] = returnValue.polygonCategorySumMap.get(id).toString();
            row[7] = returnValue.polygonArticleLengthMap.get(id).toString();
            row[8] = returnValue.polygonPageSumMap.get(id).toString();

            writer.writeNext(row);
            writer.flush();

        }



    }








}