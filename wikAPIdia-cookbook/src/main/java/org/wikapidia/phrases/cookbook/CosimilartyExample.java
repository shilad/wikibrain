package org.wikapidia.phrases.cookbook;

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
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *@author Matt Lesicko
 */
public class CosimilartyExample {

    public static void main (String args[]) throws ConfigurationException, DaoException {
        Language language = Language.getByLangCode("simple");
        Configurator c = new Configurator(new Configuration());
        LocalSRMetric sr = c.get(LocalSRMetric.class,"ESA");
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
        sr.cosimilarity(ids,ids,language);
        System.out.println(System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        sr.cosimilarity(ids,language);
        System.out.println(System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        sr.cosimilarity(names,names,language);
        System.out.println(System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        sr.cosimilarity(names,language);
        System.out.println(System.currentTimeMillis()-start);

    }
}
