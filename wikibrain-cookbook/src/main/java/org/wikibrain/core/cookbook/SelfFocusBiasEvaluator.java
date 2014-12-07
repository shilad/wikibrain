package org.wikibrain.cookbook.core;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import org.joda.time.DateTime;
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
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.spatial.cookbook.tflevaluate.DistanceMetrics;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialContainmentDao;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.dao.SpatialNeighborDao;

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
    private final PageViewDao pvDao;
    private Language lang;
    private final Env env;
    private CSVWriter output;
    private String pointLayer = "wikidata";
    private String polygonLayer;
    Map<Integer, Geometry> polygons = new HashMap<Integer, Geometry>();
    Map<Integer, TIntSet> polygonContainedId = new HashMap<Integer, TIntSet>();

    public SelfFocusBiasEvaluator(Env env,  String polygonLayer) throws ConfigurationException {
        this.env = env;
        Configurator c = env.getConfigurator();
        this.sdDao = c.get(SpatialDataDao.class);
        this.lpDao = c.get(LocalPageDao.class);
        this.upDao = c.get(UniversalPageDao.class);
        this.scDao = c.get(SpatialContainmentDao.class);
        this.llDao = c.get(LocalLinkDao.class, "matrix");
        this.lcDao = c.get(LocalCategoryMemberDao.class);
        this.rpDao = c.get(RawPageDao.class);
        this.pvDao = c.get(PageViewDao.class);
        this.polygonLayer = polygonLayer;
    }


    public retVal evaluate(Language lang) throws DaoException, FileNotFoundException, IOException{

        CSVReader reader = new CSVReader(new FileReader("gadm_matched.csv"), ',');
        List<String[]> gadmList = reader.readAll();
        for(String[] gadmItem : gadmList){
            if(Integer.parseInt(gadmItem[2]) < 2)
                GADM01Concepts.add(Integer.parseInt(gadmItem[0]));
        }


        Set<Integer> spatialUniversalPages = sdDao.getAllGeometriesInLayer("wikidata").keySet();
        Set<Integer> spatialPages = new HashSet<Integer>();
        int setCounter = 0;
        Map<Language, TIntIntMap> localLangMapInv = upDao.getAllLocalToUnivIdsMap(new LanguageSet(lang));
        Map<Language, TIntIntMap> localLangMap = new HashMap<Language, TIntIntMap>();
        for(Map.Entry<Language, TIntIntMap> entry : localLangMapInv.entrySet()){
            TIntIntMap tempMap = new TIntIntHashMap();
            TIntIntIterator iter = entry.getValue().iterator();
            while(iter.hasNext()){
                iter.advance();
                Integer value = iter.value();
                Integer key = iter.key();
                tempMap.put(value, key);
            }
            localLangMap.put(entry.getKey(), tempMap);

        }
        for(Integer p : spatialUniversalPages){
            setCounter ++;
            if(setCounter % 100000 == 0)
                System.out.printf("done loading %d out of %d\n", setCounter, spatialUniversalPages.size());
            spatialPages.add(localLangMap.get(lang).get(p));

        }

        this.lang = lang;
        //this.polygons = sdDao.getAllGeometriesInLayer(polygonLayer, "earth");
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

        Set<Integer> missingCounties = new HashSet<Integer>();


        Map<Integer, Geometry> missingMap = new HashMap<Integer, Geometry>();
		for(int i = 1; i <= 3221; i++)
			polygons.put(i, polygons.get(i));

       // for(Map.Entry<Integer, Geometry> entry : missingMap.entrySet()){
        for(Map.Entry<Integer, Geometry> entry : polygons.entrySet()){

            if(polygonContainedId.containsKey(entry.getKey())){
                containedIds = polygonContainedId.get(entry.getKey());
            }
            else{
                //containedIds = scDao.getContainedItemIds(entry.getValue(), "earth", layerSet, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);
                try{
                containedIds = scDao.getContainedItemIds(entry.getKey(), polygonLayer, RefSys.EARTH,
                    layerSet, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);
                polygonContainedId.put(entry.getKey(), containedIds);
                }
                catch(Exception e){
                    continue;
                }
            }
            int pointCounter = 0;

            LOG.info(String.format("Got %d points in polygon %d", containedIds.size(), polygonCounter));



            for(Integer c : containedIds.toArray()){
                //Skip GADM0/1
                if(GADM01Concepts.contains(c))
                    continue;
                try {

                    if(upDao.getById(c).getLocalId(lang) == -1)
                        continue;
                    Iterable<org.wikibrain.core.model.LocalLink> inlinks = llDao.getLinks(lang, upDao.getById(c).getLocalId(lang), false, true, org.wikibrain.core.model.LocalLink.LocationType.NONE);
                    Iterable<org.wikibrain.core.model.LocalLink> outlinks = llDao.getLinks(lang, upDao.getById(c).getLocalId(lang), true, true, org.wikibrain.core.model.LocalLink.LocationType.NONE);

                    int inlinkCounter = 0;
                    int outlinkCounter = 0;
                    int inlinkNSCounter = 0;
                    int outlinkNSCounter = 0;
                    int categoryCounter = 0;
                    int articleLength = 0;
                    Iterator<org.wikibrain.core.model.LocalLink> i = inlinks.iterator();
                    while (i.hasNext()){
                        org.wikibrain.core.model.LocalLink link = i.next();
                        inlinkCounter ++;

                        if(!spatialPages.contains(link.getSourceId())){
                            inlinkNSCounter++;

                        }

                    }


                    i = outlinks.iterator();
                    while (i.hasNext()){
                        org.wikibrain.core.model.LocalLink link = i.next();
                        outlinkCounter ++;

                        if(!spatialPages.contains(link.getDestId()))
                            outlinkNSCounter++;

                    }

                    if(inlinkCounter > 2000){
                        System.out.println("a ha! " + String.valueOf(c) + " " +upDao.getById(c).getBestEnglishTitle(lpDao, true).getCanonicalTitle() + " " + String.valueOf(inlinkCounter));
                    }
                    //categoryCounter = lcDao.getCategoryIds(lang, upDao.getById(c).getLocalId(lang)).size();
                    //categoryCounter = pvDao.getNumViews(lang, upDao.getById(c).getLocalId(lang), new DateTime(2014,8,1,1,0), new DateTime(2014,8,31,23,0) );
                    categoryCounter = 0;
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
        Set<Language> langSet = new HashSet<Language>();
        langSet.add(Language.EN);
        langSet.add(Language.ZH);
        
        langSet.add(Language.DE);

        langSet.add(Language.ES);
        langSet.add(Language.JA);
        langSet.add(Language.KO);
        langSet.add(Language.VI);
        langSet.add(Language.RU);
        langSet.add(Language.PL);
        langSet.add(Language.NO);
                                    


        for(Language lang: langSet){

        SelfFocusBiasEvaluator evaluator = new SelfFocusBiasEvaluator(env, "counties");
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        retVal returnValue = evaluator.evaluate(lang);
        CSVWriter writer = new CSVWriter(new FileWriter(lang.getLangCode() + "_counties.csv"), ',');
        String[] row = new String[9];
        row[0] = "Universal ID";
        row[1] = "Name";
        row[2] = "Inlink";
        row[3] = "Outlink";
        row[4] = "Inlink_NS";
        row[5] = "Outlink_NS";
        row[6] = "PageView";
        row[7] = "Length";
        row[8] = "PageCount";

        writer.writeNext(row);
        writer.flush();

        for(Integer id : returnValue.polygonPageSumMap.keySet()){
            row[0] = id.toString();
            //row[1] = upDao.getById(id).getBestEnglishTitle(lpDao, true).getCanonicalTitle();
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








}