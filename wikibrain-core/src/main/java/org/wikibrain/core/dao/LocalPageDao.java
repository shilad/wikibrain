package org.wikibrain.core.dao;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;

import java.util.Collection;
import java.util.Map;

public interface LocalPageDao<T extends LocalPage> extends Dao<T> {

    /**
     * Sets if we should try to follow the redirects or not. Default is true (to following them).
     * @param followRedirects
     */
    public void setFollowRedirects(boolean followRedirects) throws DaoException;

    /**
     * Get a single page by its title
     *
     * @param title the page's title
     * @param ns the page's namespace
     * @return the requested LocalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public T getByTitle(Title title, NameSpace ns) throws DaoException;

    /**
     * Get a single page by its id
     * @param language the page's language
     * @param pageId the page's id
     * @return the requested LocalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public T getById(Language language, int pageId) throws DaoException;


    /**
     * Gets a single page by a LocalId object
     * @param localId
     * @return
     * @throws DaoException
     */
    public T getById(LocalId localId) throws DaoException;


    /**
     * Get a set of pages by their ids
     * @param language the language of the pages
     * @param pageIds a Collection of page ids
     * @return a map of ids to pages
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) throws DaoException;

    /**
     * Get a map of pages by their titles
     * @param language the language of the pages
     * @param titles a Collection of page titles
     * @param ns the namespace of the pages
     * @return a map of titles to pages
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Title, T> getByTitles(Language language, Collection<Title> titles, NameSpace ns) throws DaoException;

    /**
     * Get an id from a title. Returns -1 if it doesn't exist.
     * @param title
     * @param language
     * @param nameSpace
     * @return
     */
    public int getIdByTitle(String title, Language language, NameSpace nameSpace) throws DaoException;


    /**
     * Get an id from a title. Returns -1 if it doesn't exist.
     * @param title
     * @return
     */
    public int getIdByTitle(Title title) throws DaoException;
}
