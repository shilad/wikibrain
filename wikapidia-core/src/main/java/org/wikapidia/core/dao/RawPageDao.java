package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.core.model.Title;

import java.util.Date;

public interface RawPageDao extends Loader<RawPage> {

    /**
     *
     * @param language
     * @param localPageId
     * @return
     * @throws DaoException
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
