package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;

public interface RawPageDao extends Loader<RawPage> {

    /**
     *
     * @param language
     * @param rawLocalPageId
     * @return
     * @throws DaoException
     */
    public RawPage getById(Language language, int rawLocalPageId) throws DaoException;

    /**
     * Returns the body (i.e. wikitext) of a particular local page.
     * @param language
     * @param rawLocalPageId
     * @return
     */
    public String getBody(Language language, int rawLocalPageId) throws DaoException;
}
