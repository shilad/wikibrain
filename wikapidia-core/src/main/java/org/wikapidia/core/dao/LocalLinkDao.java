package org.wikapidia.core.dao;


import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;

public interface LocalLinkDao extends Loader<LocalLink> {

    /**
     * Returns a single LocalLink based on language, source and destination, and the location within
     * the source article. Primarily for use by UniversalLinkDao to identify individual LocalLinks.
     * @param language
     * @param sourceId the ID of the source page
     * @param destId the ID of the destination page
     * @param location the location within the source page
     * @return a single unique LocalLink
     * @throws DaoException
     */
    public abstract LocalLink getLink(Language language, int sourceId, int destId, int location) throws DaoException;

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
    public abstract SqlDaoIterable<LocalLink> getLinks(
            Language language,
            int localId, boolean outlinks,
            boolean isParseable,
            LocalLink.LocationType locationType)
            throws DaoException;

    /**
     * get all inlinks or outlinks for a page
     * @param language
     * @param localId
     * @param outlinks true for outlinks, false for inlinks
     * @return
     * @throws DaoException
     */
    public abstract SqlDaoIterable<LocalLink> getLinks(
            Language language,
            int localId,
            boolean outlinks)
            throws DaoException;


    /**
     * Retrieve the number of links in a language meeting given criteria
     * @param language
     * @param isParseable
     * @param locationType
     * @return
     * @throws DaoException
     */
    public abstract int getNumLinks (
            Language language,
            boolean isParseable,
            LocalLink.LocationType locationType)
            throws DaoException;
}
