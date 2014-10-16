package org.wikibrain.cookbook.pageview;

import com.vividsolutions.jts.geom.Geometry;
import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class CountryPageViews {

    public static void main(String args[]) throws ConfigurationException, DaoException {
        // Configure environment
        Env env = EnvBuilder.envFromArgs(args);
        final PageViewDao viewDao = env.getConfigurator().get(PageViewDao.class);
        final LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        final SpatialDataDao spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        final Language lang = env.getDefaultLanguage();
        final UniversalPageDao conceptDao = env.getConfigurator().get(UniversalPageDao.class);
        final DateTime start = new DateTime(2014, 8, 14, 11, 0, 0);
        final DateTime end = new DateTime(2014, 8, 14, 23, 0, 0);
        viewDao.ensureLoaded(start, end,  env.getLanguages());

        // Build universal id -> country shape and local page -> shape
        Map<Integer, Geometry> conceptShapes = spatialDao.getAllGeometriesInLayer("country");
        final Map<LocalPage, Geometry> countryShapes = new HashMap<LocalPage, Geometry>();
        for (int conceptId : conceptShapes.keySet()) {
            int pageId = conceptDao.getById(conceptId).getLocalId(lang);
            LocalPage page = pageDao.getById(lang, pageId);
            if (page != null) {
                countryShapes.put(page, conceptShapes.get(conceptId));
            }
        }

        // Initialize view count by country
        final Map<LocalPage, Integer> views = new ConcurrentHashMap<LocalPage, Integer>();
        for (LocalPage p : countryShapes.keySet()) views.put(p, 0);

        final Map<Integer, Geometry> conceptPoints = spatialDao.getAllGeometriesInLayer("wikidata");
        ParallelForEach.loop(conceptPoints.keySet(), new Procedure<Integer>() {
            @Override
            public void call(Integer conceptId) throws Exception {
                LocalPage country = findCountry(countryShapes, conceptPoints.get(conceptId));
                int pageId = conceptDao.getLocalId(lang, conceptId);
                if (country == null || pageId < 0) return;  // probably in the ocean or outer space
                int n = viewDao.getNumViews(lang, pageId, start, end);
                views.put(country, views.get(country) + n);
            }
        });

        System.out.println("Views for articles contained by each country");
        for (LocalPage page : WpCollectionUtils.sortMapKeys(views, true)) {
            System.out.format("%s\t%s\n", page.getTitle().getCanonicalTitle(), views.get(page).toString());
        }
    }

    private static LocalPage findCountry(Map<LocalPage, Geometry> countryShapes, Geometry point) {
        for (LocalPage country : countryShapes.keySet()) {
            if (countryShapes.get(country).contains(point)) {
                return country;
            }
        }
        return null;
    }
}
