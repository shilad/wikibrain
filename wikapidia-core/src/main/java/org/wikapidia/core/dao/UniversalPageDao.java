package org.wikapidia.core.dao;

import gnu.trove.map.TIntIntMap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;
import java.util.Map;

/**
 */
public interface UniversalPageDao<T extends UniversalPage> extends Loader<T> {

    /**
     * Returns a UniversalPage instance of the specified page type corresponding to the input universal ID
     * @param univId the universal ID to be retrieved
     * @param algorithmId the algorithm ID of the algorithm used to generate the page
     * @return a UniversalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract T getById(int univId, int algorithmId) throws DaoException;

    /**
     * Returns a map of UniversalPages of the specified page type by a collection of universal IDs
     * @param univIds a collection of universal IDs
     * @param algorithmId the algorithm ID of the algorithm used to generate the pages
     * @return a map of universal IDs to UniversalPages
     * @throws DaoException if there was an error retrieving the pages
     */
    public abstract Map<Integer, T> getByIds(Collection<Integer> univIds, int algorithmId) throws DaoException;

    /**
     * Returns the universal ID of a local page specified by a language and
     * an ID, within the scope of the specified algorithm
     * @param language
     * @param localPageId
     * @param algorithmId
     * @return
     * @throws DaoException
     */
    public abstract int getUnivPageId(Language language, int localPageId, int algorithmId) throws DaoException;

    /**
     * Returns the universal ID of a local page, within the scope of the specified algorithm
     * @param localPage
     * @param algorithmId
     * @return
     * @throws DaoException
     */
    public abstract int getUnivPageId(LocalPage localPage, int algorithmId) throws DaoException;

    Map<Language, TIntIntMap> getAllLocalIdsToUnivIds(int algorithmId, LanguageSet ls) throws DaoException;
}
