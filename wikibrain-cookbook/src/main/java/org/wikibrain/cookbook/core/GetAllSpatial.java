package org.wikibrain.cookbook.core;

import au.com.bytecode.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.LocalPage;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by toby on 2/9/15.
 */
public class GetAllSpatial {


    public static void main(String args[]) throws Exception {
        SpatialDataDao sdDao;
        UniversalPageDao upDao;
        LocalPageDao lpDao;
        LocalLinkDao llDao;
        CSVWriter writer = new CSVWriter(new FileWriter(new File("spatiallist.csv"), true), ',');
        Env env = EnvBuilder.envFromArgs(args);
        Configurator c = env.getConfigurator();
        sdDao = c.get(SpatialDataDao.class);
        upDao = c.get(UniversalPageDao.class);
        lpDao = c.get(LocalPageDao.class);
        llDao = c.get(LocalLinkDao.class);
        String[] row = new String[3];
        Map<Integer, Geometry> geometryMap = sdDao.getAllGeometriesInLayer("wikidata");
        Integer size = geometryMap.size();
        int cnt = 0;
        for(Map.Entry<Integer, Geometry> e : geometryMap.entrySet()){
            cnt ++;
            if(cnt % 100 == 0){
                System.out.println("FINISHED " + cnt + " out of " + size);
            }
            try{
                Integer localId = upDao.getById(e.getKey()).getLocalId(Language.EN);
                Iterable<LocalLink> linkIterable = llDao.getLinks(Language.EN, localId, false);
                int i = 0;
                Iterator<LocalLink> linkIterator = linkIterable.iterator();
                while(linkIterator.hasNext()){
                    i++;
                    linkIterator.next();
                }
                if(i > 0){
                    row[0] = lpDao.getById(Language.EN, localId).getTitle().getCanonicalTitle();
                    row[1] = localId.toString();
                    row[2] = String.valueOf(i);
                    writer.writeNext(row);
                    writer.flush();
                }
            }
            catch (Exception e1){
                continue;
            }
        }

    }
}