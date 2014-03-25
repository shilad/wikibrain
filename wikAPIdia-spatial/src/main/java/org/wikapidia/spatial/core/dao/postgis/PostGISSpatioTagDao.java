package org.wikapidia.spatial.core.dao.postgis;

import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.UniversalArticle;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.spatial.core.dao.SpatioTagDao;

import java.util.Collection;
import java.util.Map;

/**
 * SpatioTagDao backed by PostGISDB
 * Created by Brent Hecht on 1/9/14.
 */
public class PostGISSpatioTagDao implements SpatioTagDao{

    private final PostGISDB db;

    // for loading
    private FastLoader fastLoader;


    public PostGISSpatioTagDao(PostGISDB db) {
        this.db = db;
    }

    @Override
    public TObjectIntMap<LocalArticle> getGeomIdsForLocalArticles(Collection<LocalArticle> las) throws DaoException {
        return null;
    }

    @Override
    public TIntObjectMap<LocalArticle> getLocalArticlesForGeomIds(Collection<Integer> geomIds) throws DaoException {
        return null;
    }

    @Override
    public TIntIntMap getUniversalArticlesForGeomIds(Collection<Integer> geomIDs) throws DaoException {
        return null;
    }

    @Override
    public TIntIntMap getGeomIdsForUniversalArticles(Collection<UniversalArticle> universalArticles) throws DaoException {
        return null;
    }

    @Override
    public void beginSaveSpatioTags() throws DaoException {
        fastLoader = new FastLoader(db.getDataSource(),"spatiotags",new String[]{"local_id","lang_id","geom_id"});
    }

    @Override
    public void endSaveSpatioTags() throws DaoException {
        fastLoader.endLoad();
        fastLoader.close();
        fastLoader = null;
    }

    @Override
    public void saveSpatioTag(SpatioTagStruct struct) throws DaoException {
        Object[] loadArr = new Object[]{struct.localId.getId(), struct.localId.getLanguage().getId(), struct.geomId};
        fastLoader.load(loadArr);
    }

    public static class Provider extends org.wikapidia.conf.Provider<PostGISSpatioTagDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SpatioTagDao.class;
        }

        @Override



        public String getPath() {
            return "spatial.dao.spatiotag";
        }

        @Override
        public PostGISSpatioTagDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {

            if (!config.getString("type").equals("sql"))
                return null;

            return new PostGISSpatioTagDao(getConfigurator().get(PostGISDB.class));

        }
    }


}
