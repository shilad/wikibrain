package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Retrieves and stores page text.
 * Wraps a LocalPageDao to build a full RawPage.
 */
public class RawPageSqlDao extends AbstractSqlDao implements RawPageDao {
    private final LocalPageDao localPageDao;

    public RawPageSqlDao(DataSource dataSource, LocalPageDao localPageDao) throws DaoException {
        super(dataSource);
        this.localPageDao = localPageDao;
    }

    @Override
    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            RawPageSqlDao.class.getResource("/db/raw-page-schema.sql")
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
    public void save(RawPage page) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.insertInto(Tables.RAW_PAGE).values(
                    page.getLang().getId(),
                    page.getPageId(),
                    page.getRevisionId(),
                    page.getBody(),
                    page.getTitle(),
                    page.getLastEdit(),
                    page.getNamespace().getArbitraryId(),
                    page.isRedirect(),
                    page.isDisambig()
            ).execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void endLoad() throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            RawPageSqlDao.class.getResource("/db/raw-page-indexes.sql")
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
    public RawPage get(Language language, int localPageId) throws DaoException {
        return buildRawPage(localPageDao.getById(language, localPageId));
    }

    private RawPage buildRawPage(LocalPage lp) throws DaoException {
        return new RawPage(lp.getLocalId(), -1,
                lp.getTitle().getCanonicalTitle(),
                getBody(lp.getLanguage(), lp.getLocalId()),
                null,
                lp.getLanguage(),
                lp.getNameSpace(),
                lp.isRedirect(),
                lp.isDisambig()
        );
    }

    private RawPage buildRawPage(Record record){
        Timestamp timestamp = record.getValue(Tables.RAW_PAGE.LASTEDIT);
        return new RawPage(record.getValue(Tables.RAW_PAGE.PAGE_ID),
                record.getValue(Tables.RAW_PAGE.REVISION_ID),
                record.getValue(Tables.RAW_PAGE.TITLE),
                record.getValue(Tables.RAW_PAGE.BODY),
                new Date(timestamp.getTime()),
                Language.getById(record.getValue(Tables.RAW_PAGE.LANG_ID)),
                NameSpace.getNameSpaceById(record.getValue(Tables.RAW_PAGE.NAME_SPACE)),
                record.getValue(Tables.RAW_PAGE.IS_REDIRECT),
                record.getValue(Tables.RAW_PAGE.IS_DISAMBIG)
        );
    }

    @Override
    public String getBody(Language language, int localPageId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            return context.
                select().
                from(Tables.RAW_PAGE).
                where(Tables.RAW_PAGE.PAGE_ID.eq(localPageId)).
                and(Tables.RAW_PAGE.LANG_ID.eq(language.getId())).
                fetchOne().
                getValue(Tables.RAW_PAGE.BODY);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    public DaoIterable<RawPage> allRawPages() throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context =  DSL.using(conn, dialect);
            TableField idField;
            Cursor<Record> result = context.select()
                    .from(Tables.RAW_PAGE)
                    .fetchLazy();
            return new DaoIterable<RawPage>(result) {
                @Override
                public RawPage transform(Record r) {
                     return buildRawPage(r);
                }
            };
        } catch (SQLException e) {
            throw new DaoException(e);
        }
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
                                config.getString("dataSource")),
                        getConfigurator().get(
                                LocalPageDao.class,
                                config.getString("localPageDao"))
                        );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
