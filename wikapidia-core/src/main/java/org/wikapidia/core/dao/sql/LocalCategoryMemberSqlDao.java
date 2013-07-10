package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Ari Weiland
 *
 * A SQL database implementation of the LocalCategoryMemberDao.
 *
 */
public class LocalCategoryMemberSqlDao extends AbstractSqlDao<LocalCategoryMember> implements LocalCategoryMemberDao {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.CATEGORY_MEMBERS.ID,
            Tables.CATEGORY_MEMBERS.LANG_ID,
            Tables.CATEGORY_MEMBERS.CATEGORY_ID,
            Tables.CATEGORY_MEMBERS.ARTICLE_ID,
    };

    public LocalCategoryMemberSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/category-members");
    }

    @Override
    public void save(LocalCategoryMember member) throws DaoException {
        insert(
            null,
            member.getLanguage().getId(),
            member.getCategoryId(),
            member.getArticleId()
        );
    }

    @Override
    public void save(LocalCategory category, LocalArticle article) throws DaoException, WikapidiaException {
        save(new LocalCategoryMember(category, article));
    }

    /**
     * This method should generally not be used.
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws DaoException
     */
    @Override
    public Iterable<LocalCategoryMember> get(DaoFilter daoFilter) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.CATEGORY_MEMBERS.LANG_ID.in(daoFilter.getLangIds()));
            }
            Cursor<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(conditions).
                    fetchLazy(getFetchSize());
            return new LocalSqlDaoIterable<LocalCategoryMember>(result, conn) {
                @Override
                public LocalCategoryMember transform(Record r) {
                    return buildLocalCategoryMember(r);
                }
            };
        } catch (SQLException e) {
            quietlyCloseConn(conn);
            throw new DaoException(e);
//        } finally {
//            quietlyCloseConn(conn);
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

    private Collection<Integer> extractIds(Result<Record> result, boolean categoryIds) {
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

    private LocalCategoryMember buildLocalCategoryMember(Record r) {
        return new LocalCategoryMember(
                r.getValue(Tables.CATEGORY_MEMBERS.CATEGORY_ID),
                r.getValue(Tables.CATEGORY_MEMBERS.ARTICLE_ID),
                Language.getById(r.getValue(Tables.CATEGORY_MEMBERS.LANG_ID))
        );
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
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalCategoryMemberSqlDao(
                        getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
