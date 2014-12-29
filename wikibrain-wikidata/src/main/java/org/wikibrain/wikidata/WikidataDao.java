package org.wikibrain.wikidata;

import org.wikibrain.core.dao.Dao;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Shilad Sen
 */
public interface WikidataDao extends Dao<WikidataStatement> {

    /**
     * Gets a property by a name in some language.
     */
    WikidataEntity getProperty(Language language, String name) throws DaoException;

    /**
     * Fetches the property associated with an id.
     * There aren't many properties (~850 as of Jan 2014), so the implementation should cache them.
     * @param id The numeric number appearing after the "P" in the Wikimedia Foundation ids.
     * @return
     * @throws DaoException
     */
    WikidataEntity getProperty(int id) throws DaoException;

    /**
     * Returns the item associated with the particular id
     * @param id The numeric number appearing after the "Q" in the Wikimedia Foundation ids.
     * @return
     * @throws DaoException
     */
    WikidataEntity getItem(int id) throws DaoException;

    /**
     * Returns all known properties. The implementation should cache them.
     * @return
     * @throws DaoException
     */
    Map<Integer, WikidataEntity> getProperties() throws DaoException;

    /**
     * Returns all statements for a particular page.
     * @param page
     * @return
     * @throws DaoException
     */
    List<WikidataStatement> getStatements(LocalPage page) throws DaoException;


    /**
     * Gets the LocalPage for the input itemId (the number after "Q") and language (if it exists)
     * @param itemId
     * @return The matching LocalPage, or null if it doesn't exist
     * @throws DaoException
     */
    org.wikibrain.core.model.UniversalPage getUniversalPage(int itemId) throws DaoException;


    /**
     * Gets the item id for a given LocalPage (the number after "Q")
     * @param page
     * @return
     * @throws DaoException
     */
    Integer getItemId(LocalPage page) throws DaoException;


    Integer getItemId(LocalId localId) throws DaoException;


    /**
     * Saves the specified entity
     * @param entity
     * @throws DaoException
     */
    public void save(WikidataEntity entity) throws DaoException;

    /**
     * Returns human-understandable interpretations of statements for a particular page.
     * They are translated into the language of the requested page.
     * @param page
     * @return
     * @throws DaoException
     */
    Map<String, List<LocalWikidataStatement>> getLocalStatements(LocalPage page) throws DaoException;

    /**
     * Returns all statements for the specified concept id.
     * @param lang Language for local statements
     * @param type Type of entity (item or property)
     * @param id numeric id (i.e. suffix after "Q" or "P")
     * @return
     * @throws DaoException
     */
    public Map<String, List<LocalWikidataStatement>> getLocalStatements(Language lang, WikidataEntity.Type type, int id) throws DaoException;

    /**
     * Translates a single language-agnostic statement into a human interpretable
     * statement in that particular language.
     *
     * @param language
     * @param statement
     * @return
     * @throws DaoException
     */
    public LocalWikidataStatement getLocalStatement(Language language, WikidataStatement statement) throws DaoException;

    String getLabel(Language language, WikidataEntity.Type type, int id) throws DaoException;

    Iterable<WikidataStatement> getByValue(WikidataEntity property, WikidataValue value) throws DaoException;

    Iterable<WikidataStatement> getByValue(String propertyName, WikidataValue value) throws DaoException;

    Set<Integer> conceptsWithValue(String propertyName, WikidataValue value) throws DaoException;

    Set<LocalId> pagesWithValue(String propertyName, WikidataValue value, Language language) throws DaoException;

    /**
     * Returns all statements that meet some sort of criterion.
     * @param filter
     * @return
     * @throws DaoException
     */
    public Iterable<WikidataStatement> get(WikidataFilter filter) throws DaoException;
}
