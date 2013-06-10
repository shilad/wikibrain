package org.wikapidia.core.dao2;

import com.sun.istack.internal.Nullable;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.Title;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: bjhecht
 */
public abstract class RedirectDao {

    /**
     * If the input title corresponds to a redirect, returns the local id of the destination of the redirect.
     * @param lang The language to be considered
     * @param t The title of the potential redirect
     * @return If t is a redirect in lang, returns the local id of the desintation of the redirect. If t is not a redirect,
     * returns null
     * @throws WikapidiaException
     */
    public abstract @Nullable Integer resolveRedirect(Language lang, Title t) throws WikapidiaException;

    /**
     * Returns true iff t is a redirect in lang
     * @param lang
     * @param t
     * @return
     * @throws WikapidiaException
     */
    public boolean isRedirect(Language lang, Title t) throws WikapidiaException{
        return (resolveRedirect(lang, t) == null);
    }

    /**
     * If the input local id corresponds to a redirect, returns the local id of the destination of the redirect.
     * @param lang The language to be considered
     * @param localId The local id of the potential redirect
     * @return If localId is a redirect in lang, returns the local id of the destination of the redirect. If localId is not a redirect,
     * returns null
     * @throws WikapidiaException
     */
    public abstract @Nullable Integer resolveRedirect(Language lang, Integer localId) throws WikapidiaException;


    public boolean isRedirect(Language lang, Integer localId) throws WikapidiaException {
        return (resolveRedirect(lang, localId) == null);
    }

    /**
     * Gets a list of redirects to an input LocalPage. Useful as a set of synonyms for a page title.
     * @param localPage
     * @return
     * @throws WikapidiaException
     */
    public abstract Set<Title> getRedirects(LocalPage localPage) throws WikapidiaException;

    /**
     * Gets all redirects for a given language;
     * @param lang
     * @return
     * @throws WikapidiaException
     */
    public abstract Set<Integer> getAllRedirectIds(Language lang) throws WikapidiaException;

    /**
     * Gets the redirect local id -> dest local id mappings for lang = langId
     * @param lang
     * @return
     * @throws WikapidiaException
     */
    public abstract Map<Integer, Integer> getAllRedirectIdsToDestIds(Language lang) throws WikapidiaException;



}
