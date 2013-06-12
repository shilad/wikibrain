package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;

public interface RawPageDao extends Loader<RawPage> {

    /**
     * Returns the RawPage associated with the page given local page.
     * @param language
     * @param localPageId
     * @return
     */
    public RawPage get(Language language, int localPageId) throws DaoException;

    /**
     * Returns the body (i.e. wikitext) of a particular local page.
     * @param language
     * @param localPageId
     * @return
     */
    public String getBody(Language language, int localPageId) throws DaoException;
}
