package org.wikapidia.wikidata;

import org.wikapidia.core.dao.Dao;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public interface WikidataDao extends Dao<WikidataStatement> {

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

    /**
     * Returns all statements that meet some sort of criterion.
     * @param filter
     * @return
     * @throws DaoException
     */
    public Iterable<WikidataStatement> get(WikidataFilter filter) throws DaoException;
}
