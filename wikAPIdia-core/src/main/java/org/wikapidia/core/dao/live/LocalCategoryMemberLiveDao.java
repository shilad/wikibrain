package org.wikapidia.core.dao.live;

import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.dao.sql.LocalCategorySqlDao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.dao.live.LiveUtils;

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

    /**
     *
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a collection of the ids of all members in the given category
     * @throws DaoException
     */
    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException {
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("CATEGORYMEMBERS", language)
                .setPageid(categoryId);
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
    public Map<Integer, LocalArticle> getCategoryMembers(Language language, int categoryId) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(language, categoryId);
        LocalArticleLiveDao dao = new LocalArticleLiveDao();
        return dao.getByIds(language, articleIds);
    }

    /**
     *
     * @param localCategory the category to find
     * @return a map contains the LocalArticleIDs and the LocalArticle of all members in the given category
     * @throws DaoException
     */
    public Map<Integer, LocalArticle> getCategoryMembers(LocalCategory localCategory) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(localCategory);
        LocalArticleLiveDao dao = new LocalArticleLiveDao();
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
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("CATEGORIES", language).setPageid(articleId);
        List<Integer> categoryIdsList = new ArrayList<Integer>();
        List<QueryReply> replies = builder.build().getValuesFromQueryResult();
        for (QueryReply reply : replies) {
            categoryIdsList.add(reply.pageId);
        }
        return  categoryIdsList;
    }

    /**
     *
     * @param localArticle the article
     * @return a collection of categoryids of all the categories this article belongs to
     * @throws DaoException
     */
    @Override
    public Collection<Integer> getCategoryIds(LocalArticle localArticle) throws DaoException {
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
    public Map<Integer, LocalCategory> getCategories(LocalArticle localArticle) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(localArticle);
        LocalCategoryLiveDao dao = new LocalCategoryLiveDao();
        return dao.getByIds(localArticle.getLanguage(), categoryIds);
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalCategoryMemberDao> {
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
        public LocalCategoryMemberDao get(String name, Config config) throws ConfigurationException {
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
