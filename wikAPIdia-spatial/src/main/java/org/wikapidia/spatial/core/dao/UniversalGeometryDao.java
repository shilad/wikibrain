package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;

/**
 * Created by bjhecht on 12/29/13.
 */
public interface UniversalGeometryDao {

    public Geometry getGeometryForPage(UniversalPage page) throws DaoException;

    public Geometry getGeometryForPages(Collection<UniversalPage> pages) throws DaoException;

}
