package org.wikibrain.core.dao;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.CategoryGraph;
import org.wikibrain.core.model.LocalCategoryMember;
import org.wikibrain.core.model.LocalPage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 * An interface that describes a Dao to determine local category membership.
 *
 * @author Shilad Sen
 * @author Ari Weiland
 *
 */
public interface LocalCategoryMemberDao extends Dao<LocalCategoryMember> {

    /**
     * Returns the best guess at the top level categories associated with a language.
     *
     * <p>
     * This first looks for a language-specific "override" specified in the reference.conf.
     * If that fails, it looks for a language-specific mapping for the
     * <a href="https://www.wikidata.org/wiki/Q4587687">Category:Main topic classifications
     * (Q4587687)</a>Wikidata concept and uses child categories in the language.
     * </p>
     *
     * <p>Be warned: This may fail for some languages.</p>
     *
     * @param language
     * @return
     * @throws DaoException
     */
    public Set<LocalPage> guessTopLevelCategories(Language language) throws DaoException;

    /**
     * Supplemental method that saves a membership relationship based on
     * a LocalCategory and LocalArticle
     * @param category a LocalCategory
     * @param article a LocalArticle that is a member of the LocalCategory
     * @throws DaoException if there was an error saving the item
     * @throws org.wikibrain.core.WikiBrainException if the category and article are in different languages
     */
    public void save(LocalPage category, LocalPage article) throws DaoException, WikiBrainException;

    public LocalPage getClosestCategory(LocalPage page, Set<LocalPage> candidates, boolean weightedDistance) throws DaoException;

    /**
     * For each article, identifies the closest category among the specified candidate set.
     * Distance is measured using shortest path in the category graph.
     *
     * @param candidateCategories   The categories to consider as candidates (e.g. those considered "top-level").
     * @param pageIds               If not null, only considers articles in the provided pageIds.
     * @param weighted              If true, use page-rank weighted edges so paths that traverse more
     *                              general categories are penalized more highly.
     * @return                      Map with candidates as keys and the articles that have them as closest category
     *                              as values. The values are a map of article ids to distances.
     * @throws DaoException
     */
    Map<LocalPage, TIntDoubleMap> getClosestCategories(Set<LocalPage> candidateCategories, TIntSet pageIds, boolean weighted) throws DaoException;


    /**
     * See #getClosestCategories with pageIds = null and weighted = true.
     * @param topLevelCats
     * @return
     */
    Map<LocalPage, TIntDoubleMap> getClosestCategories(Set<LocalPage> topLevelCats) throws DaoException;


    /**
     * Returns distances to specified categories for requested page.
     * Distance is measured using shortest path in the category graph.
     *
     * @param candidateCategories   The categories to consider as candidates (e.g. those considered "top-level").
     * @param pageId                The article id we want to find.
     * @param weighted              If true, use page-rank weighted edges so paths that traverse more
     *                              general categories are penalized more highly.
     * @return                      Map with article ids as keys and distances to each category id as values.
     * @throws DaoException
     *
     */
    TIntDoubleMap getCategoryDistances(Set<LocalPage> candidateCategories, int pageId, boolean weighted) throws DaoException;

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
    public Collection<Integer> getCategoryMemberIds(LocalPage localCategory) throws DaoException;

    /**
     * Gets a map of local articles mapped from their page IDs, based on a category
     * specified by a language and category ID
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a map of page IDs to articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalPage> getCategoryMembers(Language language, int categoryId) throws DaoException;

    /**
     * Gets a map of local articles mapped from their page IDs, based on a specified category
     * @param localCategory the category to find
     * @return a map of page IDs to articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalPage> getCategoryMembers(LocalPage localCategory) throws DaoException;

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
    public Collection<Integer> getCategoryIds(LocalPage localArticle) throws DaoException;

    /**
     * Gets a map of local categories mapped from their page IDs, based on an article
     * specified by a language and article ID
     * @param language the language of the article
     * @param articleId the article's ID
     * @return a map of page IDs to categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalPage> getCategories(Language language, int articleId) throws DaoException;

    /**
     * Gets a map of local categories mapped from their page IDs, based on a specified article
     * @param localArticle the article to find
     * @return a map of page IDs to categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalPage> getCategories(LocalPage localArticle) throws DaoException;

    /**
     * Returns a compact representation of the category graph.
     * The return value of this object is shared and cached, so caller must not change it.
     * TODO: make CategoryGraph immutable.
     * @param language
     * @return
     */
    public CategoryGraph getGraph(Language language) throws DaoException;

}
