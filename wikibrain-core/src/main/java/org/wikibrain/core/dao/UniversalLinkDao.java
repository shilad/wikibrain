package org.wikibrain.core.dao;

import gnu.trove.set.TIntSet;
import org.wikibrain.core.model.UniversalLink;
import org.wikibrain.core.model.UniversalLinkGroup;

/**
 *
 * An interface that describes a Dao to retrieve Universal Links.
 *
 * @author Ari Weiland
 *
 */
public interface UniversalLinkDao extends Dao<UniversalLink> {

    /**
     * Gets the outlinks from a given source universal ID in the form of a UniversalLinkGroup
     * @param sourceId
     * @return
     * @throws DaoException
     */
    public UniversalLinkGroup getOutlinks(int sourceId) throws DaoException;

    /**
     * Gets the inlinks from a given destination universal ID in the form of a UniversalLinkGroup
     * @param destId
     * @return
     * @throws DaoException
     */
    public UniversalLinkGroup getInlinks(int destId) throws DaoException;

    /**
     * Gets the inlinks from a given destination universal ID in the form of a TIntSet
     * @param destId
     * @return
     * @throws DaoException
     */
    public TIntSet getOutlinkIds(int destId) throws DaoException;

    /**
     * Gets the outlinks from a given source universal ID in the form of a UTIntSet
     * @param sourceId
     * @return
     * @throws DaoException
     */
    public TIntSet getInlinkIds(int sourceId) throws DaoException;

    /**
     * Gets an individual UniversalLink based on source and destination universal IDs
     * @param sourceId
     * @param destId
     * @return
     * @throws DaoException
     */
    public UniversalLink getUniversalLink(int sourceId, int destId) throws DaoException;
}
