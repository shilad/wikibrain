package org.wikapidia.core.dao;

import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A data source for
 */
public class LocalPageDao {
    private final SQLDialect dialect;
    private DataSource ds;

    public LocalPageDao(DataSource dataSource) throws SQLException {
        ds = dataSource;
        Connection conn = ds.getConnection();
        try {
            this.dialect = JooqUtils.dialect(conn);
        } finally {
            conn.close();
        }
    }

    public LocalPage get(Language lang, int localId) throws SQLException {
        Connection conn = ds.getConnection();
        try {
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.equal(localId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal((short) lang.getId())).
                    fetchOne();
            return buildPage(record);
        } finally {
            conn.close();
        }
    }

    public LocalPage get(Title title, PageType pageType) throws SQLException {
        return get(title, pageType.getNamespace());
    }

    /**
     * TODO: make this take a NameSpace enum once Rebecca commits.
     * @param title
     * @param ns
     * @return
     */
    public LocalPage get(Title title, int ns) throws SQLException {
        Connection conn = ds.getConnection();
        try {
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.equal(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal((short) title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NS.equal((short) ns)).
                    fetchOne();
            return buildPage(record);
        } finally {
            conn.close();
        }
    }

    public void beginLoad() throws SQLException {
        Connection conn = ds.getConnection();
        try {
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageDao.class.getResource("/db/local-page-schema.sql")
                    ));
        } catch (IOException e) {
            throw new SQLException(e);
        } finally {
            conn.close();
        }
    }

    public void save(LocalPage page) throws SQLException {
        Connection conn = ds.getConnection();
        try {
            DSLContext context = DSL.using(conn, dialect);
            context.insertInto(Tables.LOCAL_PAGE).values(
                    null,
                    page.getLanguage().getId(),
                    page.getLocalId(),
                    page.getTitle().getCanonicalTitle(),
                    page.getPageType().getNamespace(),
                    page.getPageType().ordinal()
            ).execute();
        } finally {
            conn.close();
        }
    }

    public void endLoad() throws SQLException {
        Connection conn = ds.getConnection();
        try {
            conn.createStatement().execute(
                    IOUtils.toString(
                        LocalPageDao.class.getResource("/db/local-page-indexes.sql")
                    ));
        } catch (IOException e) {
            throw new SQLException(e);
        } finally {
            conn.close();
        }

    }

    private LocalPage buildPage(Record record) {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.ARTICLE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        PageType ptype = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
        return new LocalPage(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title,
                ptype
        );
    }

    /**
     * Configures a local page provider. Example configuration:
     *
     * foo {
     *      type : sql,
     *      dataSource : bar
     * }
     *
     */
    public static class Provider extends org.wikapidia.conf.Provider<LocalPageDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalPageDao.class;
        }

        @Override
        public LocalPageDao get(String name, Class klass, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            String dataSourceName = config.getString("dataSource");
            DataSource dataSource = (DataSource) getConfigurator().get(DataSource.class, dataSourceName);
            try {
                return new LocalPageDao(dataSource);
            } catch (SQLException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
