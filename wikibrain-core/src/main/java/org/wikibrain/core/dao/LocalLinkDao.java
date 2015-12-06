package org.wikibrain.core.dao;


import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;

public interface LocalLinkDao extends Dao<LocalLink> {

    /**
     * Returns a single LocalLink based on language and source and destination within
     * the source article. Primarily for use by UniversalLinkDao to identify individual LocalLinks.
     * Note that this implies that two links from one page to another are considered identical.
     * @param language
     * @param sourceId the ID of the source page
     * @param destId the ID of the destination page
     * @return a single unique LocalLink
     * @throws DaoException
     *
     */

    public LocalLink getLink(Language language, int sourceId, int destId) throws DaoException;

    /**
     * Calculates the PageRank associated with a particular page.
     * Currently only implemented by the MatrixLocalLinkDao.
     * PageRank estimation is performed lazily, so the first time this method is called
     * will be very expensive, and future invocations will be cached.
     *
     * @param language
     * @param pageId
     * @return An estimate of the pageRank. The sum of PageRank values for all pages will
     * approximately sum to 1.0.
     */
    double getPageRank(Language language, int pageId);

    /**
     * Calculates the PageRank associated with a particular page.
     * Currently only implemented by the MatrixLocalLinkDao.
     * PageRank estimation is performed lazily, so the first time this method is called
     * will be very expensive, and future invocations will be cached.
     *
     * @param localId
     * @return An estimate of the pageRank. The sum of PageRank values for all pages will
     * approximately sum to 1.0.
     */
    double getPageRank(LocalId localId);

    /**
     * get all the links on a page matching criteria
     *
     * @param language
     * @param localId
     * @param outlinks true for outlinks, false for inlinks
     * @param isParseable
     * @param locationType
     * @return
     * @throws DaoException
     */
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks, boolean isParseable, LocalLink.LocationType locationType) throws DaoException;

    /**
     * get all inlinks or outlinks for a page
     *
     * @param language
     * @param localId
     * @param outlinks true for outlinks, false for inlinks
     * @return
     * @throws DaoException
     */
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks) throws DaoException;

}
