package org.wikapidia.pageview;


import gnu.trove.map.hash.TIntIntHashMap;
import org.joda.time.DateTime;
import org.wikapidia.core.lang.Language;

/**
 * @author Toby "Jiajun" Li
 */
public class PageViewExample {

    public static void main(String args[]){

        PageViewMapDao pageViewMapDao = new PageViewMapDao();
        TIntIntHashMap test1 = new TIntIntHashMap();
        test1.put(1, 5);
        test1.put(2, 3);
        test1.put(3, 10);
        pageViewMapDao.addData(new PageViewDataStruct(
                Language.getByLangCode("en"),
                new DateTime(2013,12,10,0,0),
                new DateTime(2013,12,10,1,0),
                test1
        ));
        TIntIntHashMap test2 = new TIntIntHashMap();
        test2.put(1, 2);
        test2.put(2, 7);
        test2.put(3, 1);
        pageViewMapDao.addData(new PageViewDataStruct(
                Language.getByLangCode("en"),
                new DateTime(2013,12,10,1,0),
                new DateTime(2013,12,10,2,0),
                test2
        ));
        TIntIntHashMap test3 = new TIntIntHashMap();
        test3.put(1, 2);
        test3.put(2, 0);
        test3.put(3, 20);
        pageViewMapDao.addData(new PageViewDataStruct(
                Language.getByLangCode("en"),
                new DateTime(2014,12,10,5,0),
                new DateTime(2014,12,10,6,0),
                test3
        ));
        System.out.println(pageViewMapDao.getPageView(1, new DateTime(2013,12,10,1,0)));
        System.out.println(pageViewMapDao.getPageView(2, new DateTime(2013,12,10,0,0), new DateTime(2013,12,10,2,0)));
        System.out.println(pageViewMapDao.getPageView(2, new DateTime(2013,12,10,0,0), new DateTime(2014,12,10,6,0)));
        System.out.println(pageViewMapDao.getPageView(3, new DateTime(2013,12,10,0,0), new DateTime(2014,12,25,0,0)));
    }



}
