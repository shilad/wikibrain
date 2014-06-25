package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.spatial.core.dao.SpatialContainmentDao;
import org.wikibrain.spatial.core.dao.SpatialDataDao;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by toby on 6/25/14.
 */
public class HolisticEvaluator {
    public static void main(String[] args) throws Exception {
        List<Language> langList = new LinkedList<Language>();
        langList.add(Language.getByLangCode("en"));
        langList.add(Language.getByLangCode("ar"));
        langList.add(Language.getByLangCode("ca"));
        langList.add(Language.getByLangCode("ceb"));
        langList.add(Language.getByLangCode("cs"));
        langList.add(Language.getByLangCode("de"));
        langList.add(Language.getByLangCode("es"));
        langList.add(Language.getByLangCode("fa"));
        langList.add(Language.getByLangCode("fi"));
        langList.add(Language.getByLangCode("fr"));
        langList.add(Language.getByLangCode("hu"));
        langList.add(Language.getByLangCode("id"));
        langList.add(Language.getByLangCode("it"));
        langList.add(Language.getByLangCode("ja"));
        langList.add(Language.getByLangCode("ko"));
        langList.add(Language.getByLangCode("nl"));
        langList.add(Language.getByLangCode("no"));
        langList.add(Language.getByLangCode("pl"));
        langList.add(Language.getByLangCode("pt"));
        langList.add(Language.getByLangCode("ru"));
        langList.add(Language.getByLangCode("sv"));
        langList.add(Language.getByLangCode("uk"));
        langList.add(Language.getByLangCode("vi"));
        langList.add(Language.getByLangCode("war"));
        langList.add(Language.getByLangCode("zh"));
        for(Language langu : langList){

            String lang = langu.getLangCode();

            CSVReader reader = new CSVReader(new FileReader("/Users/toby/Dropbox/Tobler2/Data/Basic TFL/TFL-"+lang+".csv"), ',');
            CSVWriter writer = new CSVWriter(new FileWriter("/Users/toby/Dropbox/Tobler2/Data/Basic TFL/TFL-"+lang+"_new.csv"), ',');
            String[] temp;

            List<EvaluationEntry> entryList = new ArrayList<EvaluationEntry>();
            List<Double> SRList = new ArrayList<Double>();
            Map<Double, Integer> SRMap = new HashMap<Double, Integer>();
            Map<Double, Integer> SRCounter = new HashMap<Double, Integer>();

            List<Double> DistList = new ArrayList<Double>();
            Map<Double, Double> DistMap = new HashMap<Double, Double>();
            Map<Double, Integer> DistCounter = new HashMap<Double, Integer>();


            /* SES */


            Env env = EnvBuilder.envFromArgs(args);
            Configurator conf = env.getConfigurator();
            SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
            SESSpaceEvaluator SESEvaluator = new SESSpaceEvaluator(env, new LanguageSet("simple"));

            //Map<Integer, Geometry> countyMap = sdDao.getAllGeometriesInLayer("counties");

            Set<String> subLayers = Sets.newHashSet();
            subLayers.add("wikidata");
            SpatialContainmentDao scDao =  conf.get(SpatialContainmentDao.class);

            TIntSet containedItemIds = scDao.getContainedItemIds(30, "country", RefSys.EARTH,
                    subLayers, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);

            LinkedList<Integer> itemIdList = new LinkedList<Integer>();
            int[] itemIds = containedItemIds.toArray();
            for(Integer k : itemIds){
                itemIdList.add(k);
            }

            Map<Integer, Geometry> geometryMap = sdDao.getBulkGeometriesInLayer(itemIdList, "wikidata", "earth");

            SESEvaluator.retrieveLocations(geometryMap, "wikidata", "states");


            //evaluator.retrieveAllLocations("wikidata", "country");
            //evaluator.evaluateSample(new File("SES.csv"), 100000);

            /* SES */


            /* Topo */
            TopoEvaluator topoEvaluator = new TopoEvaluator(env, new LanguageSet("simple"));
            topoEvaluator.retrieveLocations(geometryMap, "wikidata", "states");
            //evaluator.retrieveAllLocations("wikidata", "country");

            /* Topo */


            temp = reader.readNext();
            while((temp = reader.readNext()) != null){
                EvaluationEntry entry = new EvaluationEntry();
                entry.itemName1 = temp[0];
                entry.itemID1 = Integer.valueOf(temp[1]);
                entry.itemID2 = Integer.valueOf(temp[2]);
                entry.itemName2 = temp[3];
                entry.spatialDistance = Double.valueOf(temp[4]);
                entry.ordinalDistance = Double.valueOf(temp[5]);
                entry.SR = Double.valueOf(temp[6]);
                entry.SESDistance = SESEvaluator.evaluatePair(entry.itemID1, entry.itemID2, langu);
                entry.topoDistance = topoEvaluator.evaluatePair(entry.itemID1, entry.itemID2, langu);
                SRList.add(entry.SR);
                DistList.add(entry.ordinalDistance);
                entryList.add(entry);


            }
            Collections.sort(SRList);
            Collections.sort(DistList);

            int counter = 0;
            for(Double d : SRList){
                if(SRMap.containsKey(d)){
                    SRMap.put(d, counter + SRMap.get(d));
                    SRCounter.put(d, SRCounter.get(d) + 1);
                }
                else{
                    SRMap.put(d, counter);
                    SRCounter.put(d, 1);
                }
                counter ++;
            }

            counter = 0;
            for(Double d : DistList){
                if(DistMap.containsKey(d)){
                    DistMap.put(d, counter + DistMap.get(d));
                    DistCounter.put(d, DistCounter.get(d) + 1);
                }
                else{
                    DistMap.put(d, (double)counter);
                    DistCounter.put(d, 1);
                }
                counter ++;
                if(counter % 10000 == 0){
                    counter = counter;
                }
            }


            for(Double d : SRList){
                SRMap.put(d, SRMap.get(d) / SRCounter.get(d));
            }

            for(Double d : DistMap.keySet()){
                DistMap.put(d, Double.valueOf(DistMap.get(d).doubleValue() / DistCounter.get(d).doubleValue()));
            }


            String[] headerEntries = new String[10];
            headerEntries[0] = "ITEM_NAME_1";
            headerEntries[1] = "ITEM_ID_1";
            headerEntries[2] = "ITEM_NAME_2";
            headerEntries[3] = "ITEM_ID_2";
            headerEntries[4] = "STRAIGHT_LINE_DISTANCE";
            headerEntries[5] = "ORDINAL_DISTANCE_NORMALIZED";
            headerEntries[6] = "SES_DISTANCE";
            headerEntries[7] = "TOPO_DISTANCE_STATE";
            headerEntries[8] = "SR";
            headerEntries[9] = "SR_NORMALIZED";
            writer.writeNext(headerEntries);
            writer.flush();

            for(EvaluationEntry e : entryList){
                e.SR_percentile = SRMap.get(e.SR) / SRList.size();
                e.dist_percentile = DistMap.get(e.ordinalDistance) / DistList.size();
                String[] rowEntries = new String[10];
                rowEntries[0] = e.itemName1;
                rowEntries[1] = e.itemID1.toString();
                rowEntries[2] = e.itemName2;
                rowEntries[3] = e.itemID2.toString();
                rowEntries[4] = String.format("%.1f", e.spatialDistance);
                rowEntries[5] = String.format("%.2f", e.dist_percentile);
                rowEntries[6] = String.format("%.2f", e.SESDistance);
                rowEntries[7] = String.format("%f", e.topoDistance);
                rowEntries[8] = String.format("%.3f", e.SR);
                rowEntries[9] = String.format("%.2f", e.SR_percentile);
                writer.writeNext(rowEntries);
                writer.flush();
            }

            reader.close();
            writer.close();
        }




    }





}
