package org.wikibrain.core.dao.live;

import com.typesafe.config.Config;

import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalCategoryMemberDao;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.*;

import java.util.*;

/**
 * A Live Wiki API Implementation of LocalCategoryMemberDao
 * @author Toby "Jiajun" Li
 */
public class LocalCategoryMemberLiveDao implements LocalCategoryMemberDao{

    public LocalCategoryMemberLiveDao() throws DaoException{

    }


    //Notice: A DaoException will be thrown if you call the methods below!
    public void clear()throws DaoException{
        throw new DaoException("Can't use this method for remote wiki server!");
    }
    public void beginLoad()throws DaoException{
        throw new DaoException("Can't use this method for remote wiki server!");
    }
    public void endLoad()throws DaoException{
        throw new DaoException("Can't use this method for remote wiki server!");
    }
    public void save(LocalCategory a, LocalArticle b)throws DaoException{
        throw new DaoException("Can't use this method for remote wiki server!");
    }
    public void save(LocalCategory a, LocalPage b) throws DaoException{
        throw new DaoException("Can't use this method for remote wiki server");
    }
    public void save(LocalCategoryMember member) throws DaoException {
        throw new DaoException("Can't use this method for remote wiki server!");
    }
    public int getCount(DaoFilter a)throws DaoException{
        throw new DaoException("Can't use this method for remote wiki server!");
    }
    public Iterable<LocalCategoryMember> get(DaoFilter daoFilter) throws DaoException {
        throw new DaoException("Can't use this method for remote wiki server!");
    }
    public LanguageSet getLoadedLanguages() throws DaoException {
        throw new DaoException("Can't use this method for remote wiki server!");
    }

    //TODO: Implement getGraph
    public CategoryGraph getGraph(Language language) throws DaoException {
        throw new DaoException("Can't use this method for remote wiki server!");
    }

    /**
     *
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a collection of the ids of all members in the given category
     * @throws DaoException
     */
    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException {
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("CATEGORYMEMBERS", language)
                .addPageid(categoryId);
        List<QueryReply> replies = builder.build().getValuesFromQueryResult();
        List<Integer> categoryMemberIds = new ArrayList<Integer>();
        for (QueryReply reply : replies) {
            categoryMemberIds.add(reply.pageId);
        }
        return categoryMemberIds;
    }

    /**
     *
     * @param localCategory the category
     * @return  a collection of the ids of all members in the given category
     * @throws DaoException
     */

    public Collection<Integer> getCategoryMemberIds(LocalCategory localCategory) throws DaoException {
        return getCategoryMemberIds(localCategory.getLanguage(), localCategory.getLocalId());
    }

    /**
     *
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a map contains the LocalArticleIDs and the LocalArticle of all members in the given category
     * @throws DaoException
     */
    public Map<Integer, LocalPage> getCategoryMembers(Language language, int categoryId) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(language, categoryId);
        LocalPageLiveDao dao = new LocalPageLiveDao();
        return dao.getByIds(language, articleIds);
    }

    /**
     *
     * @param localCategory the category to find
     * @return a map contains the LocalArticleIDs and the LocalArticle of all members in the given category
     * @throws DaoException
     */
    public Map<Integer, LocalPage> getCategoryMembers(LocalCategory localCategory) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(localCategory);
        LocalPageLiveDao dao = new LocalPageLiveDao();
        return dao.getByIds(localCategory.getLanguage(), articleIds);
    }

    /**
     *
     * @param language the language of the article
     * @param articleId the articles's ID
     * @return a collection of categoryids of all the categories this article belongs to
     * @throws DaoException
     */
    public Collection<Integer> getCategoryIds(Language language, int articleId) throws DaoException {
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("CATEGORIES", language).addPageid(articleId);
        List<Integer> categoryIdsList = new ArrayList<Integer>();
        List<QueryReply> replies = builder.build().getValuesFromQueryResult();
        for (QueryReply reply : replies) {
            categoryIdsList.add(reply.pageId);
        }
        return  categoryIdsList;
    }

    /**
     *
     * @param localPage the page
     * @return a collection of categoryids of all the categories this article belongs to
     * @throws DaoException
     */
    @Override
    public Collection<Integer> getCategoryIds(LocalPage localArticle) throws DaoException {
        return getCategoryIds(localArticle.getLanguage(), localArticle.getLocalId());
    }

    /**
     *
     * @param language the language of the article
     * @param articleId the article's ID
     * @return a map contains the LocalCategoryIDs and the LocalCategories of all the categories this article belongs to
     * @throws DaoException
     */
    @Override
    public Map<Integer, LocalCategory> getCategories(Language language, int articleId) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(language, articleId);
        LocalCategoryLiveDao dao = new LocalCategoryLiveDao();
        return dao.getByIds(language, categoryIds);
    }

    /**
     *
     * @param localArticle the article to find
     * @return a map contains the LocalCategoryIDs and the LocalCategories of all the categories this article belongs to
     * @throws DaoException
     */
    @Override
    public Map<Integer, LocalCategory> getCategories(LocalPage localArticle) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(localArticle);
        LocalCategoryLiveDao dao = new LocalCategoryLiveDao();
        return dao.getByIds(localArticle.getLanguage(), categoryIds);
    }

    public static class Provider extends org.wikibrain.conf.Provider<LocalCategoryMemberDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalCategoryMemberDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localCategoryMember";
        }



        @Override
        public LocalCategoryMemberDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("live")) {
                return null;
            }
            try {
                return new LocalCategoryMemberLiveDao();
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }







}
