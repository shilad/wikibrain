package org.wikapidia.core.dao;

import com.sun.istack.internal.Nullable;
import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.Title;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: bjhecht and friends
 */
public interface RedirectDao {

    /**
     * If the input title corresponds to a redirect, returns the local id of the destination of the redirect.
     * @param lang The language to be considered
     * @param id The id of the potential redirect
     * @return If id is a redirect in lang, returns the local id of the desintation of the redirect.
     * If id is not a redirect, returns null
     * @throws WikapidiaException
     */
    public abstract @Nullable Integer resolveRedirect(Language lang, int id) throws DaoException;

    /**
     * Returns true iff id is a redirect in lang
     * @param lang
     * @param id
     * @return
     * @throws WikapidiaException
     */
    public boolean isRedirect(Language lang, int id) throws DaoException;

    /**
     * Gets a list of redirects to an input LocalPage. Useful as a set of synonyms for a page title.
     * @param localPage
     * @return
     * @throws WikapidiaException
     */
    public abstract TIntSet getRedirects(LocalPage localPage) throws DaoException;

    /**
     * Gets the redirect local id -> dest local id mappings for lang = langId
     * @param lang
     * @return
     * @throws WikapidiaException
     */
    public abstract TIntIntMap getAllRedirectIdsToDestIds(Language lang) throws DaoException;


    public abstract void save(Language lang, int src, int dest) throws DaoException;

    public abstract void update(Language lang, int src, int newDest) throws DaoException;

}
