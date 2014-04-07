package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.dao.DaoException;

import java.util.Set;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface SpatialContainmentDao {

    public static enum ContainmentOperationType {CONTAINMENT, INTERSECTION};

    public TIntSet getContainedGeomIds(Integer geomId, String refSysName,
                                            Set<String> subLayers, ContainmentOperationType opType) throws DaoException;
    public TIntSet getContainedGeomIds(Geometry g, String refSysName,
                                            Set<String> subLayers) throws DaoException;


}
