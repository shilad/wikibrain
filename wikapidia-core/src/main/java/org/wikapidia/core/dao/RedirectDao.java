package org.wikapidia.core.dao;

import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

/**
 */
public interface RedirectDao {

    /**
     * If the input id corresponds to a redirect, returns the local id of the destination of the redirect.
     * @param lang The language to be considered
     * @param id The id of the potential redirect
     * @return the local id of the destination of the redirect if it exists, else null
     * @throws DaoException
     */
    public abstract Integer resolveRedirect(Language lang, int id) throws DaoException;

    /**
     * Returns true iff id is a redirect in lang
     * @param lang
     * @param id
     * @return
     * @throws DaoException
     */
    public boolean isRedirect(Language lang, int id) throws DaoException;

    /**
     * Gets a list of redirects to an input LocalPage. Useful as a set of synonyms for a page title.
     * @param localPage
     * @return
     * @throws DaoException
     */
    public abstract TIntSet getRedirects(LocalPage localPage) throws DaoException;

    /**
     * Gets the redirect local id -> dest local id mappings for lang = langId
     * @param lang
     * @return
     * @throws DaoException
     */
    public abstract TIntIntMap getAllRedirectIdsToDestIds(Language lang) throws DaoException;


    public abstract void save(Language lang, int src, int dest) throws DaoException;

    public abstract void update(Language lang, int src, int newDest) throws DaoException;

}
