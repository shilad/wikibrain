package org.wikibrain.cookbook.pageview;

import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.pageview.PageViewSqlDao;

/**
 * @author Shilad Sen
 */
public class PageViewExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        PageViewSqlDao viewDao = env.getConfigurator().get(PageViewSqlDao.class);
        viewDao.clear();

        DateTime start = new DateTime(2014, 8, 14, 21, 0, 0);
        DateTime end = new DateTime(2014, 8, 14, 22, 0, 0);

        viewDao.ensureLoaded(start, end,  env.getLanguages());
    }
}
