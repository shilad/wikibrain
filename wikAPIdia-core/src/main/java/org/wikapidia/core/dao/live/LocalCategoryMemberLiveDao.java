package org.wikapidia.core.dao.live;

import com.typesafe.config.Config;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.dao.sql.LocalCategorySqlDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.LocalCategoryMember;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Toby "Jiajun" Li
 * Date: 11/3/13
 * Time: 12:33 AM
 * To change this template use File | Settings | File Templates.
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


    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException {
        String http = new String("http://");
        String host = new String(".wikipedia.org");
        String query = new String("/w/api.php?action=query&list=categorymembers&format=json&cmpageid=");
        List<LocalCategoryMember> categoryList = new LocalCategoryMemberQueryReply(http + language.getLangCode() + host + query + new Integer(categoryId).toString(), categoryId).getLocalCategoryMemberList();
        List<Integer> categoryMemberIdsList = new ArrayList<Integer>();
        for(LocalCategoryMember m: categoryList){
            categoryMemberIdsList.add(m.getArticleId());
        }
        return categoryMemberIdsList;
    }

    public Collection<Integer> getCategoryMemberIds(LocalCategory localCategory) throws DaoException {
        return getCategoryMemberIds(localCategory.getLanguage(), localCategory.getLocalId());
    }

    public Map<Integer, LocalArticle> getCategoryMembers(Language language, int categoryId) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(language, categoryId);
        LocalArticleLiveDao dao = new LocalArticleLiveDao();
        return dao.getByIds(language, articleIds);
    }

    public Map<Integer, LocalArticle> getCategoryMembers(LocalCategory localCategory) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(localCategory);
        LocalArticleLiveDao dao = new LocalArticleLiveDao();
        return dao.getByIds(localCategory.getLanguage(), articleIds);
    }

    public Collection<Integer> getCategoryIds(Language language, int articleId) throws DaoException {
        String http = new String("http://");
        String host = new String(".wikipedia.org");
        String query = new String("/w/api.php?action=query&prop=categories&format=json&pageids=");
        List<LocalCategory> categoryList = new LocalCategoryListQueryReply(http + language.getLangCode() + host + query + new Integer(articleId).toString()).geCategoryList();
        List<Integer> categoryIdsList = new ArrayList<Integer>();
        for(LocalCategory m: categoryList){
            categoryIdsList.add(m.getLocalId());
        }
        return categoryIdsList;
    }

    @Override
    public Collection<Integer> getCategoryIds(LocalArticle localArticle) throws DaoException {
        return getCategoryIds(localArticle.getLanguage(), localArticle.getLocalId());
    }

    @Override
    public Map<Integer, LocalCategory> getCategories(Language language, int articleId) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(language, articleId);
        LocalCategoryLiveDao dao = new LocalCategoryLiveDao();
        return dao.getByIds(language, categoryIds);
    }

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
