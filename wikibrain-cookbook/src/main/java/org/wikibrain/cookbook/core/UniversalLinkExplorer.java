package org.wikibrain.cookbook.core;

import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.UniversalLinkDao;
import org.wikibrain.core.model.UniversalLink;
import org.wikibrain.core.model.UniversalLinkGroup;

/**
 *
 * @author Ari Weiland
 */
public class UniversalLinkExplorer {

    @Test
    public void benchmarkTest() throws ConfigurationException, DaoException {
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        UniversalLinkDao dao = configurator.get(UniversalLinkDao.class, "skeletal-sql");
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
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        UniversalLinkDao dao = configurator.get(UniversalLinkDao.class, "skeletal-sql");
        long start = System.currentTimeMillis();
        UniversalLinkGroup links = dao.getOutlinks(0);
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
