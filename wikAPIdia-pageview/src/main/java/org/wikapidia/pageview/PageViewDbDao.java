package org.wikapidia.pageview;

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
    Set<Long> parsedHourSet;
    PageViewDbDao(Language lang){
        this.lang = lang;
        this.db = DBMaker.newFileDB(new File("../db/" + lang.getLangCode() + "_page_view_db")).closeOnJvmShutdown().make();
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
                if (b == 0)
                    return true;
                else if (db.exists(Integer.toString(a))) {
                    Map<Long, Integer> hourViewMap = db.getTreeMap(Integer.toString(a));
                    hourViewMap.put(dateId, b);
                    return true;
                } else {
                    Map<Long, Integer> hourViewMap = db.createTreeMap(Integer.toString(a)).make();
                    hourViewMap.put(dateId, b);
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
        if(db.exists(Integer.toString(id)) == false || !parsedHourSet.contains(dateTimeToHour(time))){
            parse(time);
        }
        if(db.exists(Integer.toString(id)) == false)
            return 0;
        Map<Long, Integer> hourViewMap = db.getTreeMap(Integer.toString(id));
        if(hourViewMap.containsKey(dateTimeToHour(time)) == false)
            return 0;
        else{

            return hourViewMap.get(dateTimeToHour(time));
        }

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
        if(db.exists(Integer.toString(id)) == false || !checkExist(startTime, endTime))
            parse(startTime, endTime);
        if(db.exists(Integer.toString(id)) == false)
            return 0;
        Map<Long, Integer> hourViewMap = db.getTreeMap(Integer.toString(id));
        for(long hrTime = dateTimeToHour(startTime); hrTime < dateTimeToHour(endTime); hrTime += 1){
            if(hourViewMap.containsKey(hrTime) == false)
                continue;
            sum += hourViewMap.get(hrTime);
        }

        return sum;

    }

    Map<Integer, Integer> getPageView(Iterable<Integer> ids, DateTime startTime, DateTime endTime)throws ConfigurationException, DaoException, WikapidiaException{
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        if(!checkExist(startTime, endTime))
            parse(startTime, endTime);
        for(Integer id: ids){
            if(db.exists(Integer.toString(id)) == false){
                result.put(id, 0);
                break; //continue?
            }
            Map<Long, Integer> hourViewMap = db.getTreeMap(Integer.toString(id));
            int sum = 0;
            for(long hrTime = dateTimeToHour(startTime); hrTime < dateTimeToHour(endTime); hrTime += 1){
                if(hourViewMap.containsKey(hrTime) == false)
                    continue;
                sum += hourViewMap.get(hrTime);
            }
            result.put(id, sum);
        }
        return result;
    }


    /**
     * Util function which gives the hour value of a given DateTime since 1970-1-1
     * @param time The DateTime object
     * @return The number of hours has passed since 2007-12-9 18:00
     */
    long dateTimeToHour(DateTime time){
        return (time.toDate().getTime() - new DateTime(2007, 12, 9, 18, 0).toDate().getTime())/3600000;
    }

    /**
     * Util function created iterator to parse page view file from startTime to endTime
     * @param startTime the specified start time
     * @param endTime  the specified end time
     * @throws ConfigurationException
     * @throws DaoException
     * @throws WikapidiaException
     */
    void parse(DateTime startTime, DateTime endTime)throws ConfigurationException, DaoException, WikapidiaException {
        PageViewIterator it = new PageViewIterator(lang, startTime, endTime);
        PageViewDataStruct data;      //int i = 0;
        while(it.hasNext()){
            data = it.next();
            addData(data);

        }


    }

    /**
     * Util function created iterator to parse view file for a single hour
     * @param time the specified hour to parse
     * @throws ConfigurationException
     * @throws DaoException
     * @throws WikapidiaException
     */
    void parse(DateTime time)throws ConfigurationException, DaoException, WikapidiaException {
        PageViewIterator it = new PageViewIterator(lang, time);
        PageViewDataStruct data;      //int i = 0;
        while(it.hasNext()){
            data = it.next();
            addData(data);

        }


    }

    /**
     *  Util function used to check if all hours in a given period have been parsed
     * @param startTime start time of a period
     * @param endTime end time of a period
     * @return
     */
    boolean checkExist(DateTime startTime, DateTime endTime){
        for(long hrTime = dateTimeToHour(startTime); hrTime < dateTimeToHour(endTime); hrTime += 1){
            if(!parsedHourSet.contains(hrTime))
                return false;
        }
        return true;
    }

}
