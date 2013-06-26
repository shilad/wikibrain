package org.wikapidia.core.dao;

import gnu.trove.map.TIntIntMap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Ari Weiland
 *
 * An interface that describes a Dao to retrieve Universal Pages.
 *
 */
public interface UniversalPageDao<T extends UniversalPage> extends Loader<T> {

    /**
     * Returns a UniversalPage instance of the specified page type corresponding to the input universal ID
     * @param univId the universal ID to be retrieved
     * @param algorithmId the algorithm ID of the algorithm used to generate the page
     * @return a UniversalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public T getById(int univId, int algorithmId) throws DaoException;

    /**
     * Returns a map of UniversalPages of the specified page type by a collection of universal IDs
     * @param univIds a collection of universal IDs
     * @param algorithmId the algorithm ID of the algorithm used to generate the pages
     * @return a map of universal IDs to UniversalPages
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, T> getByIds(Collection<Integer> univIds, int algorithmId) throws DaoException;

    /**
     * Returns the universal ID of a local page specified by a language and
     * an ID, within the scope of the specified algorithm
     * @param language
     * @param localPageId
     * @param algorithmId
     * @return
     * @throws DaoException
     */
    public int getUnivPageId(Language language, int localPageId, int algorithmId) throws DaoException;

    /**
     * Returns the universal ID of a local page, within the scope of the specified algorithm.
     * This method is SLOW and should not be used extensively. For mass usage,
     * use the getAllLocalToUnivIdsMap() method.
     * @param localPage
     * @param algorithmId
     * @return
     * @throws DaoException
     */
    public int getUnivPageId(LocalPage localPage, int algorithmId) throws DaoException;

    /**
     * Returns a group of maps between local IDs and universal IDs.
     * The maps are distributed by language.  This allows fast and easy retrieval
     * of the universal ID to which a specified algorithm mapped a page.
     * @param algorithmId the algorithm to map
     * @param ls the set of languages to map
     * @return
     * @throws DaoException
     */
    public Map<Language, TIntIntMap> getAllLocalToUnivIdsMap(int algorithmId, LanguageSet ls) throws DaoException;
}
