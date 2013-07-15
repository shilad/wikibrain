package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Retrieves and stores page text.
 * Wraps a LocalPageDao to build a full RawPage.
 */
public class RawPageSqlDao extends AbstractSqlDao<RawPage> implements RawPageDao {

    public RawPageSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/raw-page");
    }

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.RAW_PAGE.LANG_ID,
            Tables.RAW_PAGE.PAGE_ID,
            Tables.RAW_PAGE.REVISION_ID,
            Tables.RAW_PAGE.BODY,
            Tables.RAW_PAGE.TITLE,
            Tables.RAW_PAGE.LASTEDIT,
            Tables.RAW_PAGE.NAME_SPACE,
            Tables.RAW_PAGE.IS_REDIRECT,
            Tables.RAW_PAGE.IS_DISAMBIG,
            Tables.RAW_PAGE.REDIRECT_TITLE,
    };

    @Override
    public void save(RawPage page) throws DaoException {
        insert(
                page.getLanguage().getId(),
                page.getLocalId(),
                page.getRevisionId(),
                page.getBody() == null ? "" : page.getBody(),
                page.getTitle().getCanonicalTitle(),
                page.getLastEdit(),
                page.getNamespace().getArbitraryId(),
                page.isRedirect(),
                page.isDisambig(),
                page.getRedirectTitle()
        );
    }

    @Override
    public Iterable<RawPage> get(DaoFilter daoFilter) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.RAW_PAGE.LANG_ID.in(daoFilter.getLangIds()));
            }
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.RAW_PAGE.NAME_SPACE.in(daoFilter.getNameSpaceIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.RAW_PAGE.IS_REDIRECT.in(daoFilter.isRedirect()));
            }
            if (daoFilter.isDisambig() != null) {
                conditions.add(Tables.RAW_PAGE.IS_DISAMBIG.in(daoFilter.isDisambig()));
            }
            Cursor<Record> result = context.select().
                    from(Tables.RAW_PAGE).
                    where(conditions).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<RawPage>(result, conn) {
                @Override
                public RawPage transform(Record r) {
                    return buildRawPage(r);
                }
            };
        } catch (SQLException e) {
            quietlyCloseConn(conn);
            throw new DaoException(e);
        }
    }

    @Override
    public RawPage getById(Language language, int rawLocalPageId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            return buildRawPage(context.
                    select().
                    from(Tables.RAW_PAGE).
                    where(Tables.RAW_PAGE.PAGE_ID.eq(rawLocalPageId)).
                    and(Tables.RAW_PAGE.LANG_ID.eq(language.getId())).
                    fetchOne());
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public String getBody(Language language, int rawLocalPageId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            return context.
                select().
                from(Tables.RAW_PAGE).
                where(Tables.RAW_PAGE.PAGE_ID.eq(rawLocalPageId)).
                and(Tables.RAW_PAGE.LANG_ID.eq(language.getId())).
                fetchOne().
                getValue(Tables.RAW_PAGE.BODY);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    private RawPage buildRawPage(Record record){
        Timestamp timestamp = record.getValue(Tables.RAW_PAGE.LASTEDIT);
        return new RawPage(record.getValue(Tables.RAW_PAGE.PAGE_ID),
                record.getValue(Tables.RAW_PAGE.REVISION_ID),
                record.getValue(Tables.RAW_PAGE.TITLE),
                record.getValue(Tables.RAW_PAGE.BODY),
                new Date(timestamp.getTime()),
                Language.getById(record.getValue(Tables.RAW_PAGE.LANG_ID)),
                NameSpace.getNameSpaceByArbitraryId(record.getValue(Tables.RAW_PAGE.NAME_SPACE)),
                record.getValue(Tables.RAW_PAGE.IS_REDIRECT),
                record.getValue(Tables.RAW_PAGE.IS_DISAMBIG),
                record.getValue(Tables.RAW_PAGE.REDIRECT_TITLE)
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<RawPageDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<RawPageDao> getType() {
            return RawPageDao.class;
        }

        @Override
        public String getPath() {
            return "dao.rawPage";
        }

        @Override
        public RawPageDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new RawPageSqlDao(
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
