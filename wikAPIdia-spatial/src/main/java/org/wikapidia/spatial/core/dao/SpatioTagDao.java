package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.UniversalArticle;

import java.util.Collection;

/**
 * Created by Brent Hecht on 12/29/13.
 *
 *
 */
public interface SpatioTagDao {

    public TObjectIntMap<LocalArticle> getGeomIdsForLocalArticles(Collection<LocalArticle> las) throws DaoException;

    public TIntObjectMap<LocalArticle> getLocalArticlesForGeomIds(Collection<Integer> geomIds) throws DaoException;

    public TIntIntMap getUniversalArticlesForGeomIds(Collection<Integer> geomIDs) throws DaoException;

    public TIntIntMap getGeomIdsForUniversalArticles(Collection<UniversalArticle> universalArticles) throws DaoException;

    public void saveSpatioTag(SpatioTagStruct struct) throws DaoException;


    public static class SpatioTagStruct{

        public final LocalId localId;
        public final int geomId;

        public SpatioTagStruct(LocalId localId, int geomId) {
            this.localId = localId;
            this.geomId = geomId;
        }
    }


}
