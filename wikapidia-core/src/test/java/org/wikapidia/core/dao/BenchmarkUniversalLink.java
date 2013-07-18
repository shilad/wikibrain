package org.wikapidia.core.dao;

import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;

/**
 * @author Ari Weiland
 */
public class BenchmarkUniversalLink {

    @Test
    public void benchmarkTest() throws ConfigurationException {
        UniversalLinkDao dao = new Configurator(new Configuration()).get(UniversalLinkDao.class, "skeletal-sql");

    }
}
