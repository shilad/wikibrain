package org.wikapidia.core.dao;


import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;

public interface LocalLinkDao extends Loader<LocalLink> {
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
    public SqlDaoIterable<LocalLink> getLinks(Language language, int localId, boolean
            outlinks, boolean isParseable, LocalLink.LocationType locationType)
            throws DaoException;

    /**
     * get all inlinks or outlinks for a page
     * @param language
     * @param localId
     * @param outlinks true for outlinks, false for inlinks
     * @return
     * @throws DaoException
     */
    public SqlDaoIterable<LocalLink> getLinks(Language language, int localId, boolean
            outlinks) throws DaoException;

    /**
     * Get all links in a language
     * @param language
     * @param outlinks resolve as inlinks or as outlinks
     * @return
     * @throws DaoException
     */
    public SqlDaoIterable<LocalLink> getLinks(Language language, boolean outlinks) throws DaoException;


    /**
     * Retrieve the number of links in a language meeting given criteria
     * @param language
     * @param isParseable
     * @param locationType
     * @return
     * @throws DaoException
     */
    public int getNumLinks (Language language, boolean isParseable,
            LocalLink.LocationType locationType) throws DaoException;

    /**
     *  Updates based on Source ID, Lang ID and Location.
     * @param link
     * @throws DaoException
     */
    public void update(LocalLink link) throws DaoException;

    /**
     * Removes based on Source ID, Lang ID and Location.
     * @param link
     * @throws DaoException
     */
    public void remove(LocalLink link) throws DaoException;
}
