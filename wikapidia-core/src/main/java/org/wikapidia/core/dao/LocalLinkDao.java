package org.wikapidia.core.dao;


import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;

public interface LocalLinkDao extends Loader<LocalLink> {

    /**
     * Returns a single LocalLink based on language and source and destination within
     * the source article. Primarily for use by UniversalLinkDao to identify individual LocalLinks.
     * Note that this implies that two links from one page to another are considered identical.
     * @param language
     * @param sourceId the ID of the source page
     * @param destId the ID of the destination page
     * @return a single unique LocalLink
     * @throws DaoException
     */
    public LocalLink getLink(Language language, int sourceId, int destId) throws DaoException;

    /**
     * get all the links on a page matching criteria
     * @param language
     * @param localId
     * @param outlinks true for outlinks, false for inlinks
     * @param isParseable
     * @param locationType
     * @return
     * @throws DaoException
     */
    public SqlDaoIterable<LocalLink> getLinks(Language language, int localId, boolean outlinks, boolean isParseable, LocalLink.LocationType locationType) throws DaoException;

    /**
     * get all inlinks or outlinks for a page
     * @param language
     * @param localId
     * @param outlinks true for outlinks, false for inlinks
     * @return
     * @throws DaoException
     */
    public SqlDaoIterable<LocalLink> getLinks(Language language, int localId, boolean outlinks) throws DaoException;

    /**
     * Retrieve the number of links in a language meeting given criteria
     * @param language
     * @param isParseable
     * @param locationType
     * @return
     * @throws DaoException
     */
    public int getNumLinks (Language language, boolean isParseable, LocalLink.LocationType locationType) throws DaoException;
}
