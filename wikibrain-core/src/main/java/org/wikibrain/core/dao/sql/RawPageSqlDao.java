package org.wikibrain.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Retrieves and stores page text.
 * Wraps a LocalPageDao to build a full RawPage.
 */
public class RawPageSqlDao extends AbstractSqlDao<RawPage> implements RawPageDao {
    public static final int DEFAULT_FETCH_SIZE = 100;

    public RawPageSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/raw-page");
        setFetchSize(DEFAULT_FETCH_SIZE);
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
        DSLContext context = getJooq();
        try {
        Collection<Condition> conditions = getConditions(daoFilter);
        Cursor<Record> result = context.selectFrom(Tables.RAW_PAGE)
                .where(conditions)
                .limit(daoFilter.getLimitOrInfinity())
                .fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<RawPage>(result, context) {
                @Override
                public RawPage transform(Record r) {
                    return buildRawPage(r);
                }
            };
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    private Collection<Condition> getConditions(DaoFilter daoFilter) {
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
        return conditions;
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException{
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = getConditions(daoFilter);
            return context.selectCount().
                    from(Tables.RAW_PAGE).
                    where(conditions).
                    fetchOne().value1();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public RawPage getById(Language language, int rawLocalPageId) throws DaoException {
        DSLContext context = getJooq();
        try {
            return buildRawPage(context.
                    select().
                    from(Tables.RAW_PAGE).
                    where(Tables.RAW_PAGE.PAGE_ID.eq(rawLocalPageId)).
                    and(Tables.RAW_PAGE.LANG_ID.eq(language.getId())).
                    fetchOne());
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public String getBody(Language language, int rawLocalPageId) throws DaoException {
        DSLContext context = getJooq();
        try {
            return context.
                select().
                from(Tables.RAW_PAGE).
                where(Tables.RAW_PAGE.PAGE_ID.eq(rawLocalPageId)).
                and(Tables.RAW_PAGE.LANG_ID.eq(language.getId())).
                fetchOne().
                getValue(Tables.RAW_PAGE.BODY);
        } finally {
            freeJooq(context);
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

    public static class Provider extends org.wikibrain.conf.Provider<RawPageDao> {
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
        public RawPageDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new RawPageSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource"))
                        );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
