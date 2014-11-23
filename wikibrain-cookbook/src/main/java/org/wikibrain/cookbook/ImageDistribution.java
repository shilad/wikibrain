package org.wikibrain.cookbook;

import au.com.bytecode.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.LocalPage;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by toby on 10/16/14.
 */
public class ImageDistribution {
    private static final Logger LOG = Logger.getLogger(ImageDistribution.class.getName());

    public static void main(String args[]) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);
        SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
        ImageCounter imageCounter = new ImageCounter(env);
        Map<Integer, Integer> imageCountMap_G = new HashMap<Integer, Integer>();
        Map<Integer, Integer> imageCountMap_NG = new HashMap<Integer, Integer>();
        Map<Integer,Geometry> allGeomMap = sdDao.getAllGeometriesInLayer("wikidata");
        LOG.info("GET " + allGeomMap.size() + " entries");
        int counter = 0;
        CSVWriter writer = new CSVWriter(new FileWriter("ImageDistribution.csv"), ',');
        for(Integer e : allGeomMap.keySet()){
            Integer localPageId = -1;
            try{
                localPageId = upDao.getById(e).getLocalId(Language.EN);
            }
            catch (Exception e1){
                continue;
            }
            if(localPageId < 0)
                continue;
            Integer imageCount = imageCounter.getImageCount(localPageId, Language.EN, true);
            if(imageCountMap_G.containsKey(imageCount)){
                imageCountMap_G.put(imageCount, imageCountMap_G.get(imageCount) + 1);
            }
            else
                imageCountMap_G.put(imageCount, 1);
            Integer imageCount_NG = imageCounter.getImageCount(localPageId, Language.EN, false);
            if(imageCountMap_NG.containsKey(imageCount_NG)){
                imageCountMap_NG.put(imageCount_NG, imageCountMap_NG.get(imageCount_NG) + 1);
            }
            else
                imageCountMap_NG.put(imageCount_NG, 1);
            counter ++;
            if(counter % 100 == 0){
                LOG.info("DONE WITH " + counter);
            }
            //if(counter > 1000)
                //break;
        }

        String[] buffer = new String[3];
        buffer[0] = "ImageCount_G";
        buffer[1] = "PageCount";
        writer.writeNext(buffer);
        System.out.println("\n COUNTING GALLERY \n");
        for(Map.Entry<Integer, Integer> e : imageCountMap_G.entrySet()){
            buffer[0] = e.getKey().toString();
            buffer[1] = e.getValue().toString();
            writer.writeNext(buffer);
        }
        buffer[0] = "ImageCount_NG";
        buffer[1] = "PageCount";
        writer.writeNext(buffer);
        System.out.println("\n COUNTING NON_GALLERY \n");
        for(Map.Entry<Integer, Integer> e : imageCountMap_NG.entrySet()){
            buffer[0] = e.getKey().toString();
            buffer[1] = e.getValue().toString();
            writer.writeNext(buffer);
        }
        writer.flush();
        writer.close();

    }
}

