package org.wikibrain.core.dao;

import gnu.trove.map.TIntIntMap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;

import java.util.Collection;
import java.util.Map;

/**
 *
 * An interface that describes a Dao to retrieve Universal Pages.
 *
 * @author Ari Weiland
 *
 */
public interface UniversalPageDao extends Dao<UniversalPage> {

    /**
     * Returns a UniversalPage instance of the specified page type corresponding to the input universal ID
     * @param univId the universal ID to be retrieved
     * @return a UniversalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public UniversalPage getById(int univId) throws DaoException;

    /**
     * Returns a map of UniversalPages of the specified page type by a collection of universal IDs
     * @param univIds a collection of universal IDs
     * @return a map of universal IDs to UniversalPages
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, UniversalPage> getByIds(Collection<Integer> univIds) throws DaoException;

    UniversalPage getByLocalPage(LocalPage localPage) throws DaoException;

    /**
     * Returns the universal ID of a local page specified by a language and
     * an ID, within the scope of the specified algorithm
     * @param language
     * @param localPageId
     * @return
     * @throws DaoException
     */
    public int getUnivPageId(Language language, int localPageId) throws DaoException;

    /**
     * Returns the universal ID of a local page, within the scope of the specified algorithm.
     * This method is SLOW and should not be used extensively. For mass usage,
     * use the getAllLocalToUnivIdsMap() method.
     * @param localPage
     * @return
     * @throws DaoException
     */
    public int getUnivPageId(LocalPage localPage) throws DaoException;

    /**
     * Returns a group of maps between local IDs and universal IDs.
     * The maps are distributed by language.  This allows fast and easy retrieval
     * of the universal ID to which a specified algorithm mapped a page.
     * @param ls the set of languages to map
     * @return
     * @throws DaoException
     */
    public Map<Language, TIntIntMap> getAllLocalToUnivIdsMap(LanguageSet ls) throws DaoException;

    Map<Language, TIntIntMap> getAllUnivToLocalIdsMap(LanguageSet ls) throws DaoException;

    Map<Integer, Integer> getLocalIds(Language language, Collection<Integer> universalIds) throws DaoException;

    int getLocalId(Language language, int universalId) throws DaoException;
}
