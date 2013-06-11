package org.wikapidia.core.dao;


import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.PageType;

import java.util.List;

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
    public WikapidiaIterable<LocalLink> getLinks(Language language, int localId, boolean
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
    public WikapidiaIterable<LocalLink> getLinks(Language language, int localId, boolean
            outlinks) throws DaoException;


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
}
