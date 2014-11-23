package org.wikibrain.core.dao;


import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalLink;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface InterLanguageLinkDao extends Dao<InterLanguageLink> {

    /**
     * Returns all the InterLanguageLinks from a particular source.
     * @param sourceLang
     * @param sourceId the ID of the source page
     * @return a collection of the interlanguage links from the specified source.
     * @throws org.wikibrain.core.dao.DaoException
     *
     */
    public Set<LocalId> getFromSource(Language sourceLang, int sourceId) throws DaoException;

    /**
     * Returns all the InterLanguageLinks from a particular source.
     * @param source
     * @return a collection of the interlanguage links from the specified source.
     * @throws org.wikibrain.core.dao.DaoException
     */
    public Set<LocalId> getFromSource(LocalId source) throws DaoException;

    /**
     * Returns all the interlanguage links that point to the given destination
     * @param destLang
     * @param destId
     * @return
     * @throws DaoException
     */
    public Set<LocalId> getToDest(Language destLang, int destId) throws DaoException;

    /**
     * Returns all the interlanguage links that point to the given destination
     * @param dest
     * @return
     * @throws DaoException
     */
    public Set<LocalId> getToDest(LocalId dest) throws DaoException;

}
