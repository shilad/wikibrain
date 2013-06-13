package org.wikapidia.core.dao.sql;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.Loader;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 */
public class CategoryMembershipSqlDao extends AbstractSqlDao implements Loader<CategoryMembershipSqlDao.CategoryMember> {
    public CategoryMembershipSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            CategoryMembershipSqlDao.class.getResource("/db/category-members-schema.sql")
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void save(CategoryMember member) throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.insertInto(Tables.CATEGORY_MEMBERS).values(
                    null,
                    member.getCategoryId(),
                    member.getArticleId()
            ).execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    /**
     * Supplemental method that saves a membership relationship based on
     * a LocalCategory and LocalArticle
     * @param category a LocalCategory
     * @param article a LocalArticle that is a member of the LocalCategory
     * @throws DaoException if there was an error saving the item
     * @throws WikapidiaException if the category and article are in different languages
     */
    public void save(LocalCategory category, LocalArticle article) throws DaoException, WikapidiaException {
        save(new CategoryMember(category, article));
    }

    @Override
    public void endLoad() throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            CategoryMembershipSqlDao.class.getResource("/db/category-members-indexes.sql")
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    /**
     * Gets a collection of page IDs of articles that are members of the category
     * specified by the language and category ID
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a collection of page IDs of articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException{
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(Tables.CATEGORY_MEMBERS.CATEGORY_ID.eq(categoryId)).
                    and(Tables.CATEGORY_MEMBERS.LANG_ID.eq(language.getId())).
                    fetch();
            return extractIds(result, true);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    /**
     * Gets a collection of page IDs of articles that are members of the category
     * @param localCategory the category
     * @return a collection of page IDs of articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryMemberIds(LocalCategory localCategory) throws DaoException{
        return getCategoryMemberIds(localCategory.getLanguage(), localCategory.getLocalId());
    }

    /**
     * Gets a map of local articles mapped from their page IDs, based on a category
     * specified by a language and category ID
     * @param language the language of the category
     * @param categoryId the category's ID
     * @return a map of page IDs to articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalArticle> getCategoryMembers(Language language, int categoryId) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(language, categoryId);
        LocalArticleSqlDao dao = new LocalArticleSqlDao(ds);
        return dao.getByIds(language, articleIds);
    }

    /**
     * Gets a map of local articles mapped from their page IDs, based on a specified category
     * @param localCategory the category to find
     * @return a map of page IDs to articles
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalArticle> getCategoryMembers(LocalCategory localCategory) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(localCategory);
        LocalArticleSqlDao dao = new LocalArticleSqlDao(ds);
        return dao.getByIds(localCategory.getLanguage(), articleIds);
    }

    /**
     * Gets a collection of page IDs of categories that the article specified by
     * the language and category ID is a member of
     * @param language the language of the article
     * @param articleId the articles's ID
     * @return a collection of page IDs of categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryIds(Language language, int articleId) throws DaoException{
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(Tables.CATEGORY_MEMBERS.ARTICLE_ID.eq(articleId)).
                    and(Tables.CATEGORY_MEMBERS.LANG_ID.eq(language.getId())).
                    fetch();
            return extractIds(result, true);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    /**
     * Gets a collection of page IDs of categories that the article is a member of
     * @param localArticle the article
     * @return a collection of page IDs of categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Collection<Integer> getCategoryIds(LocalArticle localArticle) throws DaoException{
        return getCategoryIds(localArticle.getLanguage(), localArticle.getLocalId());
    }

    /**
     * Gets a map of local categories mapped from their page IDs, based on an article
     * specified by a language and article ID
     * @param language the language of the article
     * @param articleId the article's ID
     * @return a map of page IDs to categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalCategory> getCategories(Language language, int articleId) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(language, articleId);
        LocalCategorySqlDao dao = new LocalCategorySqlDao(ds);
        return dao.getByIds(language, categoryIds);
    }

    /**
     * Gets a map of local categories mapped from their page IDs, based on a specified article
     * @param localArticle the article to find
     * @return a map of page IDs to categories
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, LocalCategory> getCategories(LocalArticle localArticle) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(localArticle);
        LocalCategorySqlDao dao = new LocalCategorySqlDao(ds);
        return dao.getByIds(localArticle.getLanguage(), categoryIds);
    }

    protected Collection<Integer> extractIds(Result<Record> result, boolean categoryIds) {
        if (result.isEmpty()) {
            return null;
        }
        Collection<Integer> pageIds = new ArrayList<Integer>();
        for(Record record : result) {
            pageIds.add(categoryIds ?
                    record.getValue(Tables.CATEGORY_MEMBERS.CATEGORY_ID) :
                    record.getValue(Tables.CATEGORY_MEMBERS.ARTICLE_ID)
            );
        }
        return pageIds;
    }

    public class CategoryMember {
        private final int categoryId;
        private final int articleId;
        private final Language language;

        public CategoryMember(LocalCategory localCategory, LocalArticle localArticle) throws WikapidiaException {
            if (!localArticle.getLanguage().equals(localCategory.getLanguage())) {
                throw new WikapidiaException("Language Mismatch");
            }
            this.categoryId = localCategory.getLocalId();
            this.articleId = localArticle.getLocalId();
            this.language = localCategory.getLanguage();
        }

        public CategoryMember(int categoryId, int articleId, Language language) {
            this.categoryId = categoryId;
            this.articleId = articleId;
            this.language = language;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public int getArticleId() {
            return articleId;
        }

        public Language getLanguage() {
            return language;
        }
    }
}
