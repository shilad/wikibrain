package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.UniversalArticle;
import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;

/**
 * Created by Brent Hecht on 12/29/13.
 *
 *
 */
public interface SpatioTagDao {

    public Iterable<Integer> getGeomIdsForLocalArticles(Collection<LocalArticle> las) throws DaoException;

    public Iterable<LocalArticle> getLocalArticlesForGeomIds(Collection<Integer> geomIds) throws DaoException;

    public Iterable<UniversalArticle> getUniversalArticlesForGeomIds(Collection<Integer> geomIDs) throws DaoException;

    public Iterable<Integer> getGeomIdsForUniversalArticles(Collection<UniversalArticle> universalArticles) throws DaoException;

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
