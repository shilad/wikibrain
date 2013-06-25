package org.wikapidia.core.dao;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;

import java.util.Collection;
import java.util.Map;

/**
 * CategoryMembers should be parsed in the same step as LocalLinks
 */
public interface LocalCategoryMemberDao extends Loader<LocalCategoryMember> {

    /**
     * Supplemental method that saves a membership relationship based on
     * a LocalCategory and LocalArticle
     * @param category a LocalCategory
     * @param article a LocalArticle that is a member of the LocalCategory
     * @throws DaoException if there was an error saving the item
     * @throws org.wikapidia.core.WikapidiaException if the category and article are in different languages
     */
    public void save(LocalCategory category, LocalArticle article) throws DaoException, WikapidiaException;

    /**
     * Gets a collection of page IDs of articles that are members of the category
     * specified by the language and category ID
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a collection of page IDs of articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException;

    /**
     * Gets a collection of page IDs of articles that are members of the category
     * @param localCategory the category
     * @return a collection of page IDs of articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryMemberIds(LocalCategory localCategory) throws DaoException;

    /**
     * Gets a map of local articles mapped from their page IDs, based on a category
     * specified by a language and category ID
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a map of page IDs to articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalArticle> getCategoryMembers(Language language, int categoryId) throws DaoException;

    /**
     * Gets a map of local articles mapped from their page IDs, based on a specified category
     * @param localCategory the category to find
     * @return a map of page IDs to articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalArticle> getCategoryMembers(LocalCategory localCategory) throws DaoException;

    /**
     * Gets a collection of page IDs of categories that the article specified by
     * the language and category ID is a member of
     * @param language the language of the article
     * @param articleId the articles's ID
     * @return a collection of page IDs of categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryIds(Language language, int articleId) throws DaoException;

    /**
     * Gets a collection of page IDs of categories that the article is a member of
     * @param localArticle the article
     * @return a collection of page IDs of categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryIds(LocalArticle localArticle) throws DaoException;

    /**
     * Gets a map of local categories mapped from their page IDs, based on an article
     * specified by a language and article ID
     * @param language the language of the article
     * @param articleId the article's ID
     * @return a map of page IDs to categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalCategory> getCategories(Language language, int articleId) throws DaoException;

    /**
     * Gets a map of local categories mapped from their page IDs, based on a specified article
     * @param localArticle the article to find
     * @return a map of page IDs to categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalCategory> getCategories(LocalArticle localArticle) throws DaoException;

}
