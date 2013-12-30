package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.dao.DaoException;

import java.util.Map;
import java.util.Set;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface GeometryDao {

    public Map<Integer, Geometry> getGeometriesForGeomIds(Set<Integer> geomIds) throws DaoException;


}
