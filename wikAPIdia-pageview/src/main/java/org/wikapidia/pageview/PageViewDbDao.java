package org.wikapidia.pageview;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import org.joda.time.DateTime;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Toby "Jiajun" Li
 */
public class PageViewDbDao {

    DB db;
    Language lang;
    Map<Integer, TLongIntMap> idViewMap;
    Set<Long> parsedHourSet;
    PageViewDbDao(Language lang){
        this.lang = lang;
        this.db = DBMaker.newFileDB(new File("../db/" + lang.getLangCode() + "_page_view_db")).closeOnJvmShutdown().make();
        if(db.exists("idViewMap")){
            System.out.println("Table existed, reading...");
            this.idViewMap = db.getTreeMap("idViewMap");
        }
        else{
            System.out.println("Table not existed, creating...");
            this.idViewMap = db.createTreeMap("idViewMap").make();
        }
        if(db.exists("parsedHourSet"))
            this.parsedHourSet = db.getTreeSet("parsedHourSet");
        else
            this.parsedHourSet = db.createTreeSet("parsedHourSet").make();
    }




    /**
     *  Adds a PageViewDataStruct record to database
     * @param data The PageViewDataStruct being added
     */
    void addData(PageViewDataStruct data){
        final DateTime startTime = data.getStartDate();
        final Long dateId =  dateTimeToHour(data.getStartDate());
        data.getPageViewStats().forEachEntry(new TIntIntProcedure() {
            @Override
            public boolean execute(int a, int b) {
                if(b == 0)
                    return true;
                else if (idViewMap.containsKey(a)){
                    idViewMap.get(a).adjustOrPutValue(dateId, b, b);

                    return true;
                }
                else {
                    idViewMap.put(a, new TLongIntHashMap());
                    idViewMap.get(a).adjustOrPutValue(dateId, b, b);

                    return true;
                }
            }

        });
        parsedHourSet.add(dateId);
        db.commit();
    }

    /**
     * Get the number of page views for a id in an hour
     * @param id Page id
     * @param time The hour we are getting page view in
     * @return The number of page views
     */

    int getPageView(int id, DateTime time)throws ConfigurationException, DaoException, WikapidiaException{
        if(idViewMap.containsKey(id) == false || parsedHourSet.contains(dateTimeToHour(time))){
            parse(time);
        }
        if(idViewMap.containsKey(id) == false)
            return 0;
        else if(idViewMap.get(id).containsKey(dateTimeToHour(time)) == false)
            return 0;
        else
            return idViewMap.get(id).get(dateTimeToHour(time));
    }

    /**
     * Get the number of page views for a id in a given period
     * @param id Page id
     * @param startTime Start time in hour
     * @param endTime End time in hour
     * @return The number of page views
     */

    //hourly
    int getPageView(int id, DateTime startTime, DateTime endTime)throws ConfigurationException, DaoException, WikapidiaException{
        int sum = 0;
        if(idViewMap.containsKey(id) == false || !checkExist(id, startTime, endTime))
            parse(startTime, endTime);
        if(idViewMap.containsKey(id) == false)
            return 0;
        for(long hrTime = dateTimeToHour(startTime); hrTime < dateTimeToHour(endTime); hrTime += 1){
            if(idViewMap.get(id).containsKey(hrTime) == false)
                continue;
            sum += idViewMap.get(id).get(hrTime);
        }
        return sum;
    }

    /**
     * Util function which gives the hour value of a given DateTime since 1970-1-1
     * @param time The DateTime object
     * @return The number of hours has passed since 1970-1-1
     */
    long dateTimeToHour(DateTime time){
        return time.toDate().getTime()/3600000;
    }

    void parse(DateTime startTime, DateTime endTime)throws ConfigurationException, DaoException, WikapidiaException {
        PageViewIterator it = new PageViewIterator(lang, startTime, endTime);
        PageViewDataStruct data;      //int i = 0;
        while(it.hasNext()){
            data = it.next();
            addData(data);

        }
    }
    void parse(DateTime time)throws ConfigurationException, DaoException, WikapidiaException {
        PageViewIterator it = new PageViewIterator(lang, time);
        PageViewDataStruct data;      //int i = 0;
        while(it.hasNext()){
            data = it.next();
            addData(data);

        }
    }

    boolean checkExist(int id, DateTime startTime, DateTime endTime){
        for(long hrTime = dateTimeToHour(startTime); hrTime < dateTimeToHour(endTime); hrTime += 1){
            if(!parsedHourSet.contains(hrTime))
                return false;
        }
        return true;
    }

}
