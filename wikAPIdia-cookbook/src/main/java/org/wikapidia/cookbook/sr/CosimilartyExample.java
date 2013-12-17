package org.wikapidia.cookbook.sr;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *@author Matt Lesicko
 */
public class CosimilartyExample {

    public static void main (String args[]) throws ConfigurationException, DaoException {
        Configurator c = new Configurator(new Configuration());
        MonolingualSRMetric sr = c.get(MonolingualSRMetric.class,"ESA", "language", "simple");
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        
        int numpages = 1000;
        int[] ids = new int[numpages];
        String[] names = new String[numpages];
        DaoFilter daoFilter = new DaoFilter().setRedirect(false).setNameSpaces(NameSpace.ARTICLE);
        Iterator<LocalPage> pages = localPageDao.get(daoFilter).iterator();
        for (int i=0; i<numpages; i++){
           if (pages.hasNext()){
               LocalPage tempPage = pages.next();
               ids[i]= tempPage.getLocalId();
               names[i]=tempPage.getTitle().getCanonicalTitle();
           } else {
               throw new RuntimeException();
           }
        }
        long start = System.currentTimeMillis();
        sr.cosimilarity(ids,ids);
        System.out.println(System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        sr.cosimilarity(ids);
        System.out.println(System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        sr.cosimilarity(names,names);
        System.out.println(System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        sr.cosimilarity(names);
        System.out.println(System.currentTimeMillis()-start);

    }
}
