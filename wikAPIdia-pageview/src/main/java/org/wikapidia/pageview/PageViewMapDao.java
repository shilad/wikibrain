package org.wikapidia.pageview;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Toby "Jiajun" Li
 */
public class PageViewMapDao {

    //key is time (in hours), value is a map whose key is pageId, value is number of page views

    Map<Integer, TLongIntMap> idViewMap = new HashMap<Integer, TLongIntMap>();

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

    }

    /**
     * Get the number of page views for a id in an hour
     * @param id Page id
     * @param time The hour we are getting page view in
     * @return The number of page views
     */

    int getPageView(int id, DateTime time){
        if(idViewMap.containsKey(id) == false)
            return -1;
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
    int getPageView(int id, DateTime startTime, DateTime endTime){
        int sum = 0;
        if(idViewMap.containsKey(id) == false)
            return -1;
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
        return time.toDate().getTime()/360000;
    }




}
