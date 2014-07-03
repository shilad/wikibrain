package org.wikibrain.pageview;

/**
 * Created by matha004 on 7/2/14.
 */

    import java.util.ArrayList;
    import com.sun.deploy.util.BlackList;
    import gnu.trove.set.TIntSet;
    import gnu.trove.set.hash.TIntHashSet;
    import org.hibernate.annotations.SourceType;
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

    import java.io.*;
    import java.util.*;

    public class PageHitListCreator {
        private static ArrayList<Integer> blackListSet= new ArrayList<Integer>();
        private static final String blackListFilePath= "Blacklist.txt";


        public static void main(String args[]) throws ConfigurationException, DaoException, WikiBrainException, IOException {

            try{
                createBlackListSet();
            } catch (Exception e){
                System.out.println();
            }

            ArrayList<DateTime[]> dates= new ArrayList<DateTime[]>();
            for (int i = 0; i < args.length-2; i=i+2) {
                dates.add(new DateTime[]{parseDate(args[i]),(parseDate(args[i+1]))});
            }



            Env env= EnvBuilder.envFromArgs(args);
            Configurator configurator = env.getConfigurator();
            PageViewSqlDao pageViewSqlDao= configurator.get(PageViewSqlDao.class);
            LocalPageDao localPageDao = configurator.get(LocalPageDao.class);
            LocalIDToUniversalID.init(configurator);
            UniversalPageDao universalPageDao= configurator.get(UniversalPageDao.class);

            //Test #3 Get the number of page views for a list of different pages
            List<Integer> testList = new ArrayList();
            for (int id: blackListSet){
                testList.add(id);
            }

//        System.out.println(pageViewDbDao.getPageView(testList, 2014, 6, 21, 0, 15, pDao));
//        Map<Integer,Integer> results=pageViewSqlDao.getNumViews(Language.EN,testList,startTime,endTime,localPageDao);
            System.out.println("start");
            final Map<Integer,Integer> results= pageViewSqlDao.getNumViews(Language.EN,testList,dates,localPageDao); //takes a while, but doesn't seize up
            System.out.println("done creating map");
            List<Integer> keys = new ArrayList<Integer>(results.keySet());
            Collections.sort(keys, new Comparator<Integer>() {
                @Override
                public int compare(Integer k1, Integer k2) {
                    int view1 = results.get(k1);
                    int view2 = results.get(k2);

                    if(view1>view2){
                        return -1;
                    }

                    else if(view2 > view1)
                    {
                        return 1;
                    }

                    return 0;
                }
            });
//
//        for (int key : keys)
//            System.out.println(key + " :: " + results.get(key));

            System.out.println("Done sorting keys");

            try { //make sure this works
                System.out.println("Creating Hit List");
                PrintWriter pw = new PrintWriter(new FileWriter("PageHitList.txt"));
                UniversalPage page;
                String name;
                for( int i=0;i<50000;i++){ //Prints page hits in descending order
                    if(i < keys.size()){
                        page= universalPageDao.getById(LocalIDToUniversalID.translate(keys.get(i),Language.EN));
                        name=page.getBestEnglishTitle(localPageDao,true).toString();
                        if(i%10 == 0){
                            System.out.println("Writing Line " + i);
                        }
                        pw.println(page.getUnivId());
                        pw.println(name+"::" + results.get(keys.get(i)));
                    }
                }
                pw.close();
            }
            catch (Exception e){
                System.err.println("Could Not Create Hit List");
            }

        }

//

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

