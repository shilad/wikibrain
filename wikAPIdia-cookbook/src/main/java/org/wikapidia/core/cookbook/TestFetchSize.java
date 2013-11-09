package org.wikapidia.core.cookbook;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.sql.AbstractSqlDao;
import org.wikapidia.core.dao.sql.LocalLinkSqlDao;
import org.wikapidia.core.model.LocalLink;

/**
 * This program experiments with different settings for fetch size.
 * On my laptop, with a local postgres:
 * - For raw page, there is little improvement with fetch size > 100
 * - For links, there is little improvement with fetch size > 700
 * - For local pages, there is little improvement with fetch size > 100
 * @author Shilad Sen
 */
public class TestFetchSize {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = new EnvBuilder()
                .setConfigFile("../psql.conf")
                .build();
        AbstractSqlDao dao = (AbstractSqlDao) env.getConfigurator().get(
                LocalPageDao.class, "sql");
        dao.setFetchSize(1000);
        System.out.println("about to start reads");
        long before = System.currentTimeMillis();
        int i = 0;
        for (Object obj : dao.get(new DaoFilter())) {
            if (i++ % 10000 == 0) {
                System.out.println("read entity " + i);
            }
        }
        long after = System.currentTimeMillis();
        System.out.println("elapsed is " + (after - before));
    }
}
