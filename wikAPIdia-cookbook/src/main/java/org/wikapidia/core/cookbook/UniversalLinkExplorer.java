package org.wikapidia.core.cookbook;

import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.UniversalLinkDao;
import org.wikapidia.core.model.UniversalLink;
import org.wikapidia.core.model.UniversalLinkGroup;

/**
 * @author Ari Weiland
 */
public class UniversalLinkExplorer {

    @Test
    public void benchmarkTest() throws ConfigurationException, DaoException {
        UniversalLinkDao dao = new Configurator(new Configuration()).get(UniversalLinkDao.class, "skeletal-sql");
        long start = System.currentTimeMillis();
        Iterable<UniversalLink> links = dao.get(new DaoFilter());
        int i=0;
        for (UniversalLink link : links) {
            i++;
            if (i%100000==0)
                System.out.println("UniversalLinks retrieved: " + i);
        }
        long end = System.currentTimeMillis();
        long total = end - start;
        double seconds = total / 1000.0;
        System.out.println("Time (s): " + seconds);
        System.out.println("Per link (ms): " + total / (double) i);
    }

    @Test
    public void testGetOutlinks() throws ConfigurationException, DaoException {
        UniversalLinkDao dao = new Configurator(new Configuration()).get(UniversalLinkDao.class, "skeletal-sql");
        long start = System.currentTimeMillis();
        UniversalLinkGroup links = dao.getOutlinks(0, 0);
        int i=0;
        for (UniversalLink link : links) {
            i++;
            System.out.println(i + " - Dest ID: " + link.getDestId());
        }
        long end = System.currentTimeMillis();
        long total = end - start;
        double seconds = total / 1000.0;
        System.out.println("Time (s): " + seconds);
        System.out.println("Per link (ms): " + total / (double) i);
    }
}
