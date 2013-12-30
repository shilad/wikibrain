package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface LocalGeometryDao {

    public Geometry getGeometryForPage(LocalArticle article) throws DaoException;

    public Geometry getGeometryForPages(Collection<LocalArticle> article) throws DaoException;
}
