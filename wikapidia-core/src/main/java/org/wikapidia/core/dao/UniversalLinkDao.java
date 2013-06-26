package org.wikapidia.core.dao;

import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.UniversalLink;
import org.wikapidia.core.model.UniversalLinkGroup;

/**
 *
 * @author Ari Weiland
 *
 * An interface that describes a Dao to retrieve Universal Links.
 *
 */
public interface UniversalLinkDao extends Loader<UniversalLink>{

    /**
     * Alternate method for saving a UniversalLink to the database
     * @param localLink the LocalLink base of the UniversalLink
     * @param sourceUnivId the source universal ID of the link
     * @param destUnivId the destination universal ID of the link
     * @param algorithmId the algorithm ID used to generate the link
     * @throws DaoException
     */
    public void save(LocalLink localLink, int sourceUnivId, int destUnivId, int algorithmId) throws DaoException;

    /**
     * Gets the outlinks from a given source universal ID in the form of a UniversalLinkGroup
     * @param sourceId
     * @param algorithmId
     * @return
     * @throws DaoException
     */
    public UniversalLinkGroup getOutlinks(int sourceId, int algorithmId) throws DaoException;

    /**
     * Gets the inlinks from a given destination universal ID in the form of a UniversalLinkGroup
     * @param destId
     * @param algorithmId
     * @return
     * @throws DaoException
     */
    public UniversalLinkGroup getInlinks(int destId, int algorithmId) throws DaoException;

    /**
     * Gets an individual UniversalLink based on source and destination universal IDs
     * @param sourceId
     * @param destId
     * @param algorithmId
     * @return
     * @throws DaoException
     */
    public UniversalLink getUniversalLink(int sourceId, int destId, int algorithmId) throws DaoException;
}
