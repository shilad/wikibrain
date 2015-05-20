package org.wikibrain.spatial.loader;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.TCollections;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.constants.Layers;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads points from wikidata as a layer.
 *
 * @author bjhecht, Shilad
 */
public class WikidataLayerLoader {

    private static final Logger LOG = LoggerFactory.getLogger(WikidataLayerLoader.class);

    private static final int COORDINATE_LOCATION_PROPERTY_ID = 625;

    private final WikidataDao wdDao;
    private final SpatialDataDao spatialDao;
    private final MetaInfoDao miDao;

    public WikidataLayerLoader(MetaInfoDao metaDao, WikidataDao wdDao, SpatialDataDao spatialDao) {
        this.wdDao = wdDao;
        this.spatialDao = spatialDao;
        this.miDao = metaDao;
    }

    public final void loadData(final LanguageSet langs) throws DaoException {
        final TIntSet savedConcepts = TCollections.synchronizedSet(new TIntHashSet());

        final AtomicInteger matches = new AtomicInteger();
        final AtomicInteger count = new AtomicInteger();

        WikidataFilter filter = (new WikidataFilter.Builder()).withPropertyId(COORDINATE_LOCATION_PROPERTY_ID).build();
        Iterable<WikidataStatement> statements = wdDao.get(filter);
        ParallelForEach.iterate(statements.iterator(), WpThreadUtils.getMaxThreads(), 100, new Procedure<WikidataStatement>() {
            @Override
            public void call(WikidataStatement statement) throws Exception {
                try {
                    if (storeStatement(savedConcepts, langs, statement)) {
                        matches.incrementAndGet();
                    }
                } catch (Exception e) {
                    LOG.error("storage of statement failed: " + statement.toString(), e);
                    miDao.incrementErrorsQuietly(Geometry.class);
                }
                if (count.incrementAndGet() % 10000 == 0){
                    LOG.info("Matched " + matches + " out of " + count + " statements from " + this.getClass().getName());
                }
            }
        }, Integer.MAX_VALUE);
    }

    private boolean storeStatement(TIntSet savedConcepts, LanguageSet langs, WikidataStatement statement) throws DaoException {
        UniversalPage uPage = wdDao.getUniversalPage(statement.getItem().getId());
        if (uPage == null || !uPage.isInLanguageSet(langs, false)){
            return false;
        }

        int itemId = statement.getItem().getId();
        Geometry g = WikiBrainSpatialUtils.jsonToGeometry(statement.getValue().getJsonValue().getAsJsonObject());
        if (g == null) {
            return false;
        }

        if (savedConcepts.contains(itemId)) {
            return false;
        }
        savedConcepts.add(itemId);
        spatialDao.saveGeometry(itemId, Layers.WIKIDATA, RefSys.EARTH,  g);
        miDao.incrementRecords(Geometry.class);
        return true;
    }
}
