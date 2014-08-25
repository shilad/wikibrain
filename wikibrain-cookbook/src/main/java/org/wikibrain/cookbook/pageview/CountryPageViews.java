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
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpCollectionUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class CountryPageViews {
    private final Env env;
    private final PageViewDao viewDao;
    private final SpatialDataDao spatialDao;
    private final Language lang;
    private final UniversalPageDao conceptDao;
    private final Map<Integer, Geometry> countryShapes;
    private final HashMap<Integer, LocalPage> countryPages;
    private final WikidataDao wikidataDao;
    private final DateTime start;
    private final DateTime end;

    public CountryPageViews(Env env) throws ConfigurationException, DaoException {
        this.env = env;
        this.viewDao = env.getConfigurator().get(PageViewDao.class);
        this.spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        this.lang = env.getDefaultLanguage();
        this.conceptDao = env.getConfigurator().get(UniversalPageDao.class);
        this.wikidataDao = env.getConfigurator().get(WikidataDao.class);

        // Download and import pageview stats if necessary.
        start = new DateTime(2014, 8, 14, 11, 0, 0);
        end = new DateTime(2014, 8, 14, 23, 0, 0);
        viewDao.ensureLoaded(start, end,  env.getLanguages());

        // Build universal id -> country shape
        this.countryShapes = spatialDao.getAllGeometriesInLayer("country");

        // Build universal id -> local page
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.countryPages = new HashMap<Integer, LocalPage>();
        for (int conceptId : countryShapes.keySet()) {
            int pageId = conceptDao.getById(conceptId).getLocalId(lang);
            LocalPage page = pageDao.getById(lang, pageId);
            if (page != null) {
                countryPages.put(conceptId, page);
            }
        }
    }

    /**
     * Shows direct views for all articles in each country.
     * @throws DaoException
     */
    public void showCountryArticleViews() throws DaoException {
        Map<LocalPage, Integer> views = new HashMap<LocalPage, Integer>();
        for (int conceptId : countryPages.keySet()) {
            LocalPage page = countryPages.get(conceptId);
            if (page != null) {
                views.put(countryPages.get(conceptId), viewDao.getNumViews(page.toLocalId(), start, end));
            }
        }

        System.out.println("Views for each country's article");
        for (LocalPage page : WpCollectionUtils.sortMapKeys(views, true)) {
            System.out.format("%s\t%s\n", page.getTitle().getCanonicalTitle(), views.get(page).toString());
        }
    }

    /**
     * Shows direct views for all articles in each country.
     * @throws DaoException
     */
    public void showViewsContainedInCountry() throws DaoException {
        // Initial view count by country
        final Map<LocalPage, Integer> views = new ConcurrentHashMap<LocalPage, Integer>();
        for (LocalPage p : countryPages.values()) views.put(p, 0);

        final Map<Integer, Geometry> conceptPoints = spatialDao.getAllGeometriesInLayer("wikidata");
        ParallelForEach.loop(conceptPoints.keySet(), new Procedure<Integer>() {
            @Override
            public void call(Integer conceptId) throws Exception {
                LocalPage country = findCountry(conceptPoints.get(conceptId));
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

    public void showCountryPopulations() throws DaoException {
        for (int conceptId : countryPages.keySet()) {
            WikidataFilter filter = new WikidataFilter.Builder()
                    .withPropertyId(1082)
                    .withEntityId(conceptId).build();
            int maxPopulation = 0;
            for (WikidataStatement stm : wikidataDao.get(filter)) {
                maxPopulation = Math.max(stm.getValue().getIntValue(), maxPopulation);
            }
            System.out.format("%s\t%d\n", countryPages.get(conceptId).getTitle().getCanonicalTitle(), maxPopulation);
        }
    }

    private LocalPage findCountry(Geometry point) {
        for (int countryId : countryShapes.keySet()) {
            if (countryShapes.get(countryId).contains(point)) {
                return countryPages.get(countryId);
            }
        }
        return null;
    }

    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        CountryPageViews cpv = new CountryPageViews(env);

//        cpv.showCountryArticleViews();
//        cpv.showViewsContainedInCountry();
        cpv.showCountryPopulations();
    }
}
