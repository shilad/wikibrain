package org.wikibrain.cookbook.sr;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.SRMetric;

import java.util.Iterator;

/**
 *@author Matt Lesicko
 */
public class CosimilartyExample {

    public static void main (String args[]) throws ConfigurationException, DaoException {
        Env env = new EnvBuilder().build();
        Configurator c = env.getConfigurator();
        SRMetric sr = c.get(SRMetric.class,"ESA", "language", "simple");
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
