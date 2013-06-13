package org.wikapidia.core.dao.sql;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategoryMember;
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
public class LocalCategoryMemberSqlDao extends AbstractSqlDao implements LocalCategoryMemberDao {
    public LocalCategoryMemberSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalCategoryMemberSqlDao.class.getResource("/db/category-members-schema.sql")
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
    public void save(LocalCategoryMember member) throws DaoException {
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

    @Override
    public void save(LocalCategory category, LocalArticle article) throws DaoException, WikapidiaException {
        save(new LocalCategoryMember(category, article));
    }

    @Override
    public void endLoad() throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalCategoryMemberSqlDao.class.getResource("/db/category-members-indexes.sql")
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
    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(Tables.CATEGORY_MEMBERS.CATEGORY_ID.eq(categoryId)).
                    and(Tables.CATEGORY_MEMBERS.LANG_ID.eq(language.getId())).
                    fetch();
            return extractIds(result, false);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public Collection<Integer> getCategoryMemberIds(LocalCategory localCategory) throws DaoException {
        return getCategoryMemberIds(localCategory.getLanguage(), localCategory.getLocalId());
    }

    @Override
    public Map<Integer, LocalArticle> getCategoryMembers(Language language, int categoryId) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(language, categoryId);
        LocalArticleSqlDao dao = new LocalArticleSqlDao(ds);
        return dao.getByIds(language, articleIds);
    }

    @Override
    public Map<Integer, LocalArticle> getCategoryMembers(LocalCategory localCategory) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(localCategory);
        LocalArticleSqlDao dao = new LocalArticleSqlDao(ds);
        return dao.getByIds(localCategory.getLanguage(), articleIds);
    }

    @Override
    public Collection<Integer> getCategoryIds(Language language, int articleId) throws DaoException {
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

    @Override
    public Collection<Integer> getCategoryIds(LocalArticle localArticle) throws DaoException {
        return getCategoryIds(localArticle.getLanguage(), localArticle.getLocalId());
    }

    @Override
    public Map<Integer, LocalCategory> getCategories(Language language, int articleId) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(language, articleId);
        LocalCategorySqlDao dao = new LocalCategorySqlDao(ds);
        return dao.getByIds(language, categoryIds);
    }

    @Override
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
}
