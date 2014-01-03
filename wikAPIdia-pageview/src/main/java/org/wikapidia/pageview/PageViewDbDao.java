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
     * method to access a PageViewIterator via the DAO, can be used by clients to keep track of each of the PageViewDataStructs
     * retrieved by the iterator
     * @param lang
     * @param startYear
     * @param startMonth
     * @param startDay
     * @param startHour
     * @param numHours
     * @return
     * @throws WikapidiaException
     * @throws DaoException
     */
    public PageViewIterator getPageViewIterator(Language lang, int startYear, int startMonth, int startDay, int startHour,
                                                int numHours) throws WikapidiaException, DaoException {
        return new PageViewIterator(lang, startYear, startMonth, startDay, startHour, numHours);
    }


    /**
     *  Adds a PageViewDataStruct record to database
     * @param data The PageViewDataStruct being added
     */
    public void addData(PageViewDataStruct data){
        final Long dateId =  data.getStartDate().getMillis();

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
     * @param year The year we are getting page view in
     * @param month The month we are getting page view in
     * @param day The day we are getting page view in
     * @param hour The hour we are getting page view in
     * @return The number of page views
     */
    public int getPageView(int id, int year, int month, int day, int hour)throws ConfigurationException, DaoException, WikapidiaException{
        DateTime time = new DateTime(year, month, day, hour, 0);
        if(!parsedHourSet.contains(time.getMillis())){
            parse(time);
        }
        if(db.exists(Integer.toString(id)) == false)
            return 0;
        Map<Long, Integer> hourViewMap = db.getTreeMap(Integer.toString(id));
        if(hourViewMap.containsKey(time.getMillis()) == false)
            return 0;
        else{

            return hourViewMap.get(time.getMillis());
        }

    }

    /**
     * Get the number of page views for a id in a given period
     * @param id Page id
     * @param startYear
     * @param startMonth
     * @param startDay
     * @param startHour
     * @param numHours Number of hours from the start date specified by the above parameters; defines the time period
     * @return The number of page views
     */

    //hourly
    public int getPageView(int id, int startYear, int startMonth, int startDay, int startHour, int numHours) throws ConfigurationException, DaoException, WikapidiaException{
        int sum = 0;
        DateTime startTime = new DateTime(startYear, startMonth, startDay, startHour, 0);
        DateTime endTime = startTime.plusHours(numHours);
        if(!checkExist(startTime, endTime))
            parse(startTime, numHours);
        if(db.exists(Integer.toString(id)) == false)
            return 0;
        Map<Long, Integer> hourViewMap = db.getTreeMap(Integer.toString(id));
        for(DateTime hrTime = startTime; hrTime.getMillis() < endTime.getMillis(); hrTime = hrTime.plusHours(1)){
            if(hourViewMap.containsKey(hrTime.getMillis()) == false)
                continue;
            sum += hourViewMap.get(hrTime.getMillis());
        }

        return sum;

    }

    public Map<Integer, Integer> getPageView(Iterable<Integer> ids, int startYear, int startMonth, int startDay, int startHour,
                                      int numHours) throws ConfigurationException, DaoException, WikapidiaException{
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        DateTime startTime = new DateTime(startYear, startMonth, startDay, startHour, 0);
        DateTime endTime = startTime.plusHours(numHours);
        if(!checkExist(startTime, endTime))
            parse(startTime, numHours);
        for(Integer id: ids){
            if(db.exists(Integer.toString(id)) == false){
                result.put(id, 0);
                continue;
            }
            Map<Long, Integer> hourViewMap = db.getTreeMap(Integer.toString(id));
            int sum = 0;
            for(DateTime hrTime = startTime; hrTime.getMillis() < endTime.getMillis(); hrTime = hrTime.plusHours(1)){
                if(hourViewMap.containsKey(hrTime.getMillis()) == false)
                    continue;
                sum += hourViewMap.get(hrTime.getMillis());
            }
            result.put(id, sum);
        }
        return result;
    }

    /**
     * Util function created iterator to parse page view file from startTime through a given number of hours
     * @param startTime the specified start time
     * @param numHours  the specified number of hours from startTime for which to parse page view files
     * @throws ConfigurationException
     * @throws DaoException
     * @throws WikapidiaException
     */
    void parse(DateTime startTime, int numHours)throws ConfigurationException, DaoException, WikapidiaException {
        PageViewIterator it = new PageViewIterator(lang, startTime.getYear(), startTime.getMonthOfYear(),
                startTime.getDayOfMonth(), startTime.getHourOfDay(), numHours);
        PageViewDataStruct data;
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
        PageViewDataStruct data;
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
        for(DateTime hrTime = startTime; hrTime.getMillis() < endTime.getMillis(); hrTime = hrTime.plusHours(1)){
            if(!parsedHourSet.contains(hrTime.getMillis()))
                return false;
        }
        return true;
    }

}
