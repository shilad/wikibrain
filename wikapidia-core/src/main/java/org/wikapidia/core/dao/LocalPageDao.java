package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.util.Collection;
import java.util.Map;

public interface LocalPageDao<T extends LocalPage> extends Loader<T> {

    /**
     * Returns an Iterable of LocalPages that fit the filters specified by the PageFilter.
     * Possible filters are what languages and what namespaces to search over, as well as
     * whether or not to search for redirect or disambiguation pages. Filters set to null
     * will not limit the search.
     * @param pageFilter a set of filters to limit the search
     * @return an Iterable of local pages that fit the specified filters
     * @throws DaoException if there was an error retrieving the pages
     */
    public abstract SqlDaoIterable<T> get(PageFilter pageFilter) throws DaoException;

    /**
     * Sets if we should try to follow the redirects or not. Default is true (to following them).
     * @param followRedirects
     */
    public abstract void setFollowRedirects(boolean followRedirects) throws DaoException;

    /**
     * Get a single page by its title
     * @param language the page's language
     * @param title the page's title
     * @param ns the page's namespace
     * @return the requested LocalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract T getByTitle(Language language, Title title, NameSpace ns) throws DaoException;

    /**
     * Get a single page by its title
     * @param language the page's language
     * @param pageId the page's id
     * @return the requested LocalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract T getById(Language language, int pageId) throws DaoException;

    /**
     * Get a set of pages by their ids
     * @param language the language of the pages
     * @param pageIds a Collection of page ids
     * @return a map of ids to pages
     * @throws DaoException if there was an error retrieving the pages
     */
    public abstract Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) throws DaoException;

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
     * Get an id from a title
     * @param title
     * @param language
     * @param nameSpace
     * @return
     */
    public int getIdByTitle(String title, Language language, NameSpace nameSpace) throws DaoException;
}
