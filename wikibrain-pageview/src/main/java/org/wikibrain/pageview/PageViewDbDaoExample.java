package org.wikibrain.pageview;

import com.sun.deploy.util.BlackList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalIDToUniversalID;
import org.wikibrain.core.model.UniversalPage;
import sun.reflect.generics.reflectiveObjects.LazyReflectiveObjectGenerator;
import sun.rmi.runtime.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Toby "Jiajun" Li
 */
public class PageViewDbDaoExample {
    private static ArrayList<Integer> blackListSet= new ArrayList<Integer>();
    private static final String blackListFilePath= "Blacklist.txt";


    public static void main(String args[]) throws ConfigurationException, DaoException, WikiBrainException {

        try{
            createBlackListSet();
        } catch (Exception e){
            System.out.println();
        }

        ArrayList<DateTime[]> dates= new ArrayList<DateTime[]>();
            for (int i = 0; i < 9; i=i+2) {
                dates.add(new DateTime[]{parseDate(args[i]),(parseDate(args[i+1]))});
            }



//        PageViewDbDao pageViewDbDao = new PageViewDbDao(Language.SIMPLE);
        Env env= EnvBuilder.envFromArgs(args);
        Configurator configurator = env.getConfigurator();
        PageViewSqlDao pageViewSqlDao= configurator.get(PageViewSqlDao.class);
        LocalPageDao localPageDao = configurator.get(LocalPageDao.class);
        LocalIDToUniversalID.init(configurator);
        UniversalPageDao universalPageDao= configurator.get(UniversalPageDao.class);

        DateTime startTime= new DateTime(2014,6,21,0,0); //set the start date to year, month,day, hour, min
        DateTime endTime= new DateTime(2014,6,21,23,0);

       // Test #1 Get the number of views by local page ID, startTime, endTime for sqlDao and by year, month, day, start hour, end hour for dbDao
//        System.out.println("UniID "+ LocalIDToUniversalID.translate(219587) + " : " + pageViewDbDao.getPageView(219587, 2014, 6, 21, 1, 10, localPageDao));
//        System.out.println("UniID " + LocalIDToUniversalID.translate(219587) + " : " + pageViewSqlDao.getNumViews(Language.SIMPLE, 219587, startTime,endTime, localPageDao));

        // Test #2 Get numb of view per hour and add them up manually
//        int sum = 0;
//        for(int i = 0; i < 24; i++ ){ //Get the hourly pageview
//            int numbOfViews=pageViewSqlDao.getNumViews(Language.SIMPLE,219587,startTime,endTime,localPageDao);
//            int numbOfViews=pageViewDbDao.getPageView(47, 2014, 6, 21, i, pDao);
//            System.out.printf("%d:00: %d\n", i,numbOfViews);
//            sum += numbOfViews;
//        }
//        System.out.printf("sum: %d\n", sum);

        //Test #3 Get the number of page views for a list of different pages
       List<Integer> testList = new ArrayList();
       for (int id: blackListSet){
           testList.add(id);
       }
        //edit for push
//        testList.add(219587);
//        testList.add(47);
//        testList.add(39);
//        testList.add(10983);
//        testList.add(858);

//        System.out.println(pageViewDbDao.getPageView(testList, 2014, 6, 21, 0, 15, pDao));
        System.out.println("he");
        Map<Integer,Integer> results=pageViewSqlDao.getNumViews(Language.SIMPLE,testList,startTime,endTime,localPageDao);
        System.out.println("start");
//        Map<Integer,Integer> results=pageViewSqlDao.getNumViews(Language.SIMPLE,testList,dates,localPageDao);
        System.out.println("done creating map");
        try {
//            PrintWriter pw = new PrintWriter(new FileWriter("PageHitList.txt"));
            for( int i=50;i<2000;i++){ //Prints page hits in ascending order (very slow)
                for(Integer key: results.keySet()){
                    if(results.get(key)==i){
                        UniversalPage page= universalPageDao.getById(LocalIDToUniversalID.translate(key),1);
                        String name=page.getBestEnglishTitle(localPageDao,true).toString();
//                        pw.println(page.getUnivId());
//                        pw.println(name+"\t" + results.get(key));
                    }
                }

            }

//            for (Integer i:results.keySet()){ //Prints all page hits in random order
//                if(results.get(i)>0) {
//                    String name=universalPageDao.getById(LocalIDToUniversalID.translate(i),1).getBestEnglishTitle(localPageDao,true).toString();
//                    pw.println(name+" :: "+LocalIDToUniversalID.translate(i) + " :: " + results.get(i));
//                }
//            }

//        pw.close();
        }
        catch (Exception e){

        }

    }

    private static void createBlackListSet() throws FileNotFoundException {
            File file = new File(blackListFilePath);
            Scanner scanner = new Scanner(file);
            while(scanner.hasNext()){
                blackListSet.add(scanner.nextInt());
            }

            scanner.close();

    }

    private static DateTime parseDate(String dateString) throws WikiBrainException {
        if (dateString == null) {
            throw new WikiBrainException("Need to specify start and end date");
        }
        String[] dateElems = dateString.split("-");
        for (String de: dateElems){
//            System.out.println(de);
        }
        try {
            int year = Integer.parseInt(dateElems[0]);
            int month = Integer.parseInt(dateElems[1]);
            int day = Integer.parseInt(dateElems[2]);
            int hour = Integer.parseInt(dateElems[3]);
            return new DateTime(year, month, day, hour, 0);
        } catch (Exception e) {
            System.out.println(e.toString());
            throw new WikiBrainException("Start and end dates must be entered in the following format (hyphen-delimited):\n" +
                    "<four_digit_year>-<numeric_month_1-12>-<numeric_day_1-31>-<numeric_hour_0-23>");
        }
    }
}
