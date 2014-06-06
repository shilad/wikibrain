package org.wikibrain.spatial.cookbook;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.io.IOUtils;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.Dao;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.core.dao.SpatialDataDao;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by toby on 6/6/14.
 */
public class MapQuestTravelTimeCalculator {

    String key;
    String startingPoint;
    String destPoint;
    JsonParser parser;
    private static final Logger LOG = Logger.getLogger(MapQuestTravelTimeCalculator.class.getName());
    int keyCounter = 0;
    List<String> keys = new ArrayList<String>();



    public MapQuestTravelTimeCalculator(){
        keys.add("Fmjtd%7Cluur2g68n5%2Cas%3Do5-9a8gqf");
        this.parser = new JsonParser();
        this.key = keys.get(keyCounter);
    }

    public String pointToString(Geometry g){
        Point gc = g.getCentroid();
        return "{latLng:{lat:" + gc.getY() +",lng:" + gc.getX() + "}}";
    }

    public void setKey(String key){
        this.key = key;
    }

    public void setStartingPoint(String startingPoint){
        this.startingPoint = startingPoint;
    }

    public void  setDestPoint(String destPoint){
        this.destPoint = destPoint;
    }

    public double getTravelTime() throws DaoException{
        if(key == null){
            throw new DaoException("Null MapQuest key");
        }
        if(startingPoint == null || destPoint == null){
            throw new DaoException("Null staring point or end point");
        }
        String queryUrl = "http://www.mapquestapi.com/directions/v2/route?key=" + key + "&from=" + startingPoint + "&to=" + destPoint;
        String info = new String();
        InputStream inputStr;
        try{
            inputStr = new URL(queryUrl).openStream();
            try {
                info = IOUtils.toString(inputStr);
            }
            catch(Exception e){
                throw new DaoException("Error parsing MapQuest query URL");
            }
            finally {
                IOUtils.closeQuietly(inputStr);
            }
        }
        catch(Exception e){
            throw new DaoException("Error getting page from the MapQuest Server (Check your internet connection) ");
        }
        double time = -1;

        try {
            String statusCode = parser.parse(info).getAsJsonObject().get("info").getAsJsonObject().get("statuscode").getAsString();
            if(statusCode.equals("0"))
                time = parser.parse(info).getAsJsonObject().get("route").getAsJsonObject().get("time").getAsDouble();
            //unable to route
            if(statusCode.equals("400"))
                time = -1;
            if(statusCode.equals("403")){
                //time = -1;
                LOG.warning("MapQuest key error, try to switch to the next key");
                if(keyCounter < keys.size() - 1){
                    keyCounter ++;
                    key = keys.get(keyCounter);
                    return getTravelTime();
                }
                else{
                    keyCounter = -1;
                    throw new DaoException("No more available key");
                }


            }
        }
        catch (DaoException e) {

            if(keyCounter == -1)
                throw e;
            //else do nothing


        }

        //System.out.println(info);

        return time;
    }

    public static void main(String args[]) throws Exception{
        Env env = EnvBuilder.envFromArgs(args);

        // Get data access objects
        Configurator c = env.getConfigurator();
        SpatialDataDao sdDao = c.get(SpatialDataDao.class);
        LocalPageDao lpDao = c.get(LocalPageDao.class);
        UniversalPageDao upDao = c.get(UniversalPageDao.class);

        MapQuestTravelTimeCalculator travelTime = new MapQuestTravelTimeCalculator();

        String mpls = travelTime.pointToString(sdDao.getGeometry("Minneapolis", Language.getByLangCode("simple"), "wikidata"));
        String nyc = travelTime.pointToString(sdDao.getGeometry("New York", Language.getByLangCode("simple"), "wikidata"));

        travelTime.setStartingPoint(mpls);
        travelTime.setDestPoint(nyc);

        System.out.println(travelTime.getTravelTime());

    }
}
