package org.wikibrain.core.dao;

import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Redirect;

/**
 */
public interface RedirectDao extends Dao<Redirect> {

    /**
     * Alternate method for saving a redirect to the database
     * @param lang
     * @param src
     * @param dest
     * @throws DaoException
     */
    public void save(Language lang, int src, int dest) throws DaoException;

    /**
     * If the input id corresponds to a redirect, returns the local id of the destination of the redirect.
     * @param lang The language to be considered
     * @param id The id of the potential redirect
     * @return the local id of the destination of the redirect if it exists, else null
     * @throws DaoException
     */
    public Integer resolveRedirect(Language lang, int id) throws DaoException;

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
    public TIntSet getRedirects(LocalPage localPage) throws DaoException;

    /**
     * Gets the redirect local id -&gt; dest local id mappings for lang = langId
     * @param lang
     * @return
     * @throws DaoException
     */
    public TIntIntMap getAllRedirectIdsToDestIds(Language lang) throws DaoException;

}
