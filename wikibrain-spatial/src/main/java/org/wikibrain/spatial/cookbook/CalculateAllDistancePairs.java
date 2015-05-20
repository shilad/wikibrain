package org.wikibrain.spatial.cookbook;

import au.com.bytecode.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.wikidata.WikidataDao;
import gnu.trove.map.TIntObjectMap;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by toby on 4/15/14.
 */
public class CalculateAllDistancePairs {

    private static final Logger LOG = LoggerFactory.getLogger(CalculateAllDistancePairs.class);

    public static void main(String[] args) throws Exception {
            File f=new File("./distance_output.csv");
            //Change this if we'll use more than 25 languages - otherwise it will overflow
            String[] entries = new String[55];
            CSVWriter csvWriter = new CSVWriter(new FileWriter(f), ',');

            Env env = EnvBuilder.envFromArgs(args);
            Configurator c = env.getConfigurator();

            SpatialDataDao sdDao = c.get(SpatialDataDao.class);
            WikidataDao wdDao = c.get(WikidataDao.class);
            LocalPageDao lpDao = c.get(LocalPageDao.class);


            //TODO: modify this field if we want multi-language
            LanguageSet langs = env.getLanguages();


            Map<Integer, SRMetric> langIdEnsembleSRMetricMap = new HashMap<Integer, SRMetric>();
            Map<Integer, SRMetric> langIdInlinkSRMetricMap = new HashMap<Integer, SRMetric>();
            for(Language lang : langs.getLanguages()){
                langIdEnsembleSRMetricMap.put(new Integer(lang.getId()), c.get(SRMetric.class, "ensemble", "language", lang.getLangCode()));
                langIdInlinkSRMetricMap.put(new Integer(lang.getId()), c.get(SRMetric.class, "inlink", "language", lang.getLangCode()));
            }

            Map<Integer, Geometry> idGeomMap = sdDao.getAllGeometriesInLayer("wikidata", "earth");
            TIntObjectMap<String> idNameMap = new TIntObjectHashMap<String>();
            LOG.info(String.format("Get %d geometries, now building id-name mapping", idGeomMap.size()));
            int counter1 = 0;
            for(Integer wdItem : idGeomMap.keySet()){
                counter1 ++;
                if(counter1 % 1000 == 0)
                    LOG.info(String.format("Finish building name mapping for %d items", counter1));
                boolean containAllLanguage = true;
                for(Language language : langs.getLanguages()){
                    if(! lpDao.getLoadedLanguages().containsLanguage(language)){
                        throw new DaoException(String.format("Language %s not loaded", language.getEnLangName()));
                    }
                    if(wdDao.getItem(wdItem).getLabels().get(language) == null){
                        containAllLanguage = false;
                        break;

                    }

                }
                if (!containAllLanguage)
                    continue;

                //TODO: This one should be changed if we switch to full English
                String name = wdDao.getItem(wdItem).getLabels().get(langs.getDefaultLanguage());
                if(name == null)
                    continue;

                idNameMap.put(wdItem, name);
            }
            LOG.info(String.format("Finish building id-name mapping for %d entities", idNameMap.size()));

            GeodeticCalculator calc = new GeodeticCalculator();


            int counter = 0;
            int[] keyArray = idNameMap.keySet().toArray();
            int Max = idNameMap.keySet().size()-1;


            entries[0] = "ITEM_NAME_1";
            entries[1] = "ITEM_ID_1";
            entries[2] = "ITEM_NAME_2";
            entries[3] = "ITEM_ID_2";
            entries[4] = "SPATIAL_DISTANCE";
            int lang_counter = 0;
            List<Language> langList = new ArrayList<Language>();
            for(Language language : langs.getLanguages())
                langList.add(language);
            for(Language language : langList){
                entries[5 + 2 * lang_counter] = "SR_ENSEMBLE_" + language.getLangCode();
                entries[6 + 2 * lang_counter] = "SR_INLINK_" + language.getLangCode();
                lang_counter ++;
            }
            csvWriter.writeNext(entries);


            //TODO: Number of data pairs we want

            for(counter = 0; counter < 1000; counter ++){
                if (counter % 100 == 0){
                    LOG.info(String.format("Finish calculating %d pairs", counter));
                    csvWriter.flush();
                }
                int x1 = (int)(Math.random() * (Max + 1));
                int x2 = (int)(Math.random() * (Max + 1));
                Integer item1 = new Integer(keyArray[x1]);
                Integer item2 = new Integer(keyArray[x2]);
                try{
                    Geometry g1 = sdDao.getGeometry(item1, "wikidata", "earth");
                    Point centroid = g1.getCentroid();
                    calc.setStartingGeographicPoint(centroid.getX(), centroid.getY());
                    Geometry g2 = sdDao.getGeometry(item2, "wikidata", "earth");
                    centroid = g2.getCentroid();
                    calc.setDestinationGeographicPoint(centroid.getX(), centroid.getY());
                    entries[0] = idNameMap.get(item1);
                    entries[1] = item1.toString();
                    entries[2] = idNameMap.get(item2);
                    entries[3] = item2.toString();
                    entries[4] = new Double(calc.getOrthodromicDistance()/1000).toString();
                    lang_counter = 0;
                    for(Language language : langList){
                        int pageId1 = lpDao.getIdByTitle(wdDao.getItem(item1).getLabels().get(language), language, NameSpace.ARTICLE);
                        int pageId2 = lpDao.getIdByTitle(wdDao.getItem(item2).getLabels().get(language), language, NameSpace.ARTICLE);
                        try{
                            entries[5 + 2 * lang_counter] = String.valueOf(langIdEnsembleSRMetricMap.get(new Integer(language.getId())).similarity(pageId1, pageId2, false).getScore());
                        }
                        catch (Exception e){
                            entries[5 + 2 * lang_counter] = "ERROR";
                        }
                        try {
                            entries[6 + 2 * lang_counter] = String.valueOf(langIdInlinkSRMetricMap.get(new Integer(language.getId())).similarity(pageId1, pageId2, false).getScore());
                        }
                        catch (Exception e){
                            entries[6 + 2 * lang_counter] = "ERROR";
                        }
                        lang_counter ++;

                    }
                    csvWriter.writeNext(entries);
                }
                catch (Exception e){
                    csvWriter.writeNext(entries);
                    csvWriter.flush();
                    //do nothing
                }

            }


            /*
            for(Integer item1 : idGeomMap.keySet()){
                counter1 ++;
                if(counter1 % 1 == 0)
                    LOG.log(Level.INFO, String.format("Finish calculating for wikidata %d", counter1));

                int counter2 = 0;
                Geometry g1 = sdDao.getGeometry(item1, "wikidata", "earth");
                Point centroid = g1.getCentroid();
                calc.setStartingGeographicPoint(centroid.getX(), centroid.getY());

                for(Integer item2 : idGeomMap.keySet()){
                    counter2 ++;
                    if(counter2 % 1000 == 0)
                        LOG.log(Level.INFO, String.format("Finish calculating %d pairs for wikidata %d",counter2, counter1));
                    Geometry g2 = sdDao.getGeometry(item2, "wikidata", "earth");
                    centroid = g2.getCentroid();
                    calc.setDestinationGeographicPoint(centroid.getX(), centroid.getY());
                    entries[0] = idNameMap.get(item1);
                    entries[1] = item1.toString();
                    entries[2] = idNameMap.get(item2);
                    entries[3] = item2.toString();
                    entries[4] = new Double(calc.getOrthodromicDistance()/1000).toString();
                    csvWriter.writeNext(entries);


                }
                csvWriter.flush();
            }
            */


            csvWriter.writeNext(entries);
            csvWriter.close();

    }


}
