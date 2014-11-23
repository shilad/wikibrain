package org.wikibrain.cookbook.pageview;

import gnu.trove.map.TIntIntMap;
import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.pageview.PageViewSqlDao;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.Arrays;

/**
 * @author Shilad Sen
 */
public class PageViewExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        // Get the pageview dao
        Env env = EnvBuilder.envFromArgs(args);
        PageViewDao viewDao = env.getConfigurator().get(PageViewDao.class);
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        Language lang = env.getDefaultLanguage();

        // Download and import pageview stats if necessary.
        DateTime start = new DateTime(2014, 8, 14, 11, 0, 0);
        DateTime end = new DateTime(2014, 8, 14, 23, 0, 0);
        viewDao.ensureLoaded(start, end,  env.getLanguages());

        // Retrieve counts for all pageviews
        TIntIntMap allViews = viewDao.getAllViews(lang, start, end);
        int pageIds[] = WpCollectionUtils.sortMapKeys(allViews, true);
        System.out.println("Top pageviews in " + lang);
        for (int i = 0; i < 10; i++) {
            LocalPage page = pageDao.getById(lang, pageIds[i]);
            int n = allViews.get(pageIds[i]);
            System.out.format("%d. %s (nviews=%d)\n", (i+1), page.getTitle(), n);
        }
    }
}
