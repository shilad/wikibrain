package org.wikibrain.spatial.loader;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataSqlDao;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bjhecht on 4/1/14.
 */
public abstract class WikidataLayerLoader {


    protected final WikidataDao wdDao;
    protected final SpatialDataDao spatialDao;
    private final MetaInfoDao miDao;

    public static final String EARTH_REF_SYS_NAME = "earth";

    private static final Logger LOG = Logger.getLogger(WikidataLayerLoader.class.getName());


    public WikidataLayerLoader(WikidataDao wdDao, SpatialDataDao spatialDao) {
        this.wdDao = wdDao;
        this.spatialDao = spatialDao;
        this.miDao = null;
    }

    protected abstract WikidataFilter getWikidataFilter();

    public final void loadData(final LanguageSet langs) throws WikiBrainException {

        try {
            final AtomicInteger matches = new AtomicInteger();
            final AtomicInteger count = new AtomicInteger();
            Iterable<WikidataStatement> statements = wdDao.get(getWikidataFilter());
            ParallelForEach.iterate(statements.iterator(), WpThreadUtils.getMaxThreads(), 100, new Procedure<WikidataStatement>() {
                @Override
                public void call(WikidataStatement statement) throws Exception {
                    UniversalPage uPage = wdDao.getUniversalPage(statement.getItem().getId());
                    if (uPage != null && uPage.isInLanguageSet(langs, false)){
                        matches.incrementAndGet();
                        try {
                            storeStatement(statement);
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, "storage of statement failed: " + statement.toString(), e);
                        }
                    }

                    count.incrementAndGet();
                    if (count.get() % 10000 == 0){
                        LOG.log(Level.INFO, "Matched " + matches + " out of " + count + " statements from " + this.getClass().getName());
                    }

                }
            }, Integer.MAX_VALUE);

        }catch(DaoException e){
            throw new WikiBrainException(e);
        }

    }

    protected abstract boolean storeStatement(WikidataStatement statement) throws DaoException;


}
