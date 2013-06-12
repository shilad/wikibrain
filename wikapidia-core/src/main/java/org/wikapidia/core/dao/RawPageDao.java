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
     * @param revisionId
     * @param body
     * @param title
     * @param lastEdit
     * @param pageType
     * @return
     * @throws DaoException
     */
    public RawPage get(Language language, int localPageId, int revisionId, String body, Title title,
                       Date lastEdit, PageType pageType) throws DaoException;

    /**
     * Returns the body (i.e. wikitext) of a particular local page.
     * @param language
     * @param localPageId
     * @return
     */
    public String getBody(Language language, int localPageId) throws DaoException;
}
