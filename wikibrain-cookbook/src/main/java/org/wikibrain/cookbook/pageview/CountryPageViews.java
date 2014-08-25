package org.wikibrain.cookbook.pageview;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.TIntIntMap;
import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class CountryPageViews {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        // Setup environment
        Env env = EnvBuilder.envFromArgs(args);
        PageViewDao viewDao = env.getConfigurator().get(PageViewDao.class);
        SpatialDataDao spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        UniversalPageDao conceptDao = env.getConfigurator().get(UniversalPageDao.class);
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        Language lang = env.getDefaultLanguage();

        // Download and import pageview stats if necessary.
        DateTime start = new DateTime(2014, 8, 14, 21, 0, 0);
        DateTime end = new DateTime(2014, 8, 14, 23, 0, 0);
        viewDao.ensureLoaded(start, end,  env.getLanguages());

        Map<Integer, Geometry> countries = spatialDao.getAllGeometriesInLayer("country");
        System.out.println("Found " + countries.size() + " countries");

        for (int conceptId : countries.keySet()) {
            int pageId = conceptDao.getById(conceptId).getLocalId(lang);
            if (pageId > 0) {
                LocalPage page = pageDao.getById(lang, pageId);
                System.out.println("Country is " + countries);
            }
        }
    }
}
