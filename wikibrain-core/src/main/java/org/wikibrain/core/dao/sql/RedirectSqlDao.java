package org.wikibrain.core.dao.sql;

import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.RedirectDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 */
public class
        RedirectSqlDao extends AbstractSqlDao<Redirect> implements RedirectDao {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.REDIRECT.LANG_ID,
            Tables.REDIRECT.SRC_PAGE_ID,
            Tables.REDIRECT.DEST_PAGE_ID,
    };

    public RedirectSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/redirect");
    }

    @Override
    public void save(Redirect redirect) throws DaoException {
        insert(
                redirect.getLanguage().getId(),
                redirect.getSourceId(),
                redirect.getDestId()
        );
    }

    @Override
    public void save(Language lang, int src, int dest) throws DaoException {
        save(new Redirect(lang, src, dest));
    }

    /**
     * Generally this method should not be used.
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws DaoException
     */
    @Override
    public Iterable<Redirect> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.REDIRECT.LANG_ID.in(daoFilter.getLangIds()));
            }
            Cursor<Record> result = context.select().
                    from(Tables.REDIRECT).
                    where(conditions).
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<Redirect>(result, context) {
                @Override
                public Redirect transform(Record r) {
                    return buildRedirect(r);
                }
            };
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException{
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.REDIRECT.LANG_ID.in(daoFilter.getLangIds()));
            }
            return context.select().
                    from(Tables.REDIRECT).
                    where(conditions).
                    fetchCount();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Integer resolveRedirect(Language lang, int id) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select().from(Tables.REDIRECT)
                    .where(Tables.REDIRECT.SRC_PAGE_ID.equal(id))
                    .and(Tables.REDIRECT.LANG_ID.equal(lang.getId()))
                    .fetchOne();
            if (record == null){
                return null;
            }
            return record.getValue(Tables.REDIRECT.DEST_PAGE_ID);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public boolean isRedirect(Language lang, int id) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select().from(Tables.REDIRECT)
                    .where(Tables.REDIRECT.SRC_PAGE_ID.equal(id))
                    .and(Tables.REDIRECT.LANG_ID.equal(lang.getId()))
                    .fetchOne();
            return (record != null);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public TIntSet getRedirects(LocalPage localPage) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().
                    from(Tables.REDIRECT).
                    where(Tables.REDIRECT.DEST_PAGE_ID.eq(localPage.getLocalId())).
                    and(Tables.REDIRECT.LANG_ID.eq(localPage.getLanguage().getId())).
                    fetch();
            TIntSet ids = new TIntHashSet();
            for (Record record : result){
                ids.add(record.getValue(Tables.REDIRECT.SRC_PAGE_ID));
            }
            return ids;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public TIntIntMap getAllRedirectIdsToDestIds(Language lang) throws DaoException {
        DSLContext context = getJooq();
        try {
            Cursor<Record> cursor = context.select().
                    from(Tables.REDIRECT).
                    where(Tables.REDIRECT.LANG_ID.equal(lang.getId())).
                    fetchLazy(getFetchSize());
            TIntIntMap ids = new TIntIntHashMap(
                    gnu.trove.impl.Constants.DEFAULT_CAPACITY,
                    gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR,
                    -1, -1);
            for (Record record : cursor){
                ids.put(record.getValue(Tables.REDIRECT.SRC_PAGE_ID),
                        record.getValue(Tables.REDIRECT.DEST_PAGE_ID));
            }
            return ids;
        } finally {
            freeJooq(context);
        }
    }

    private Redirect buildRedirect(Record r) {
        if (r == null){
            return null;
        }
        return new Redirect(
                Language.getById(r.getValue(Tables.REDIRECT.LANG_ID)),
                r.getValue(Tables.REDIRECT.SRC_PAGE_ID),
                r.getValue(Tables.REDIRECT.DEST_PAGE_ID)
        );
    }

    public static class Provider extends org.wikibrain.conf.Provider<RedirectSqlDao>  {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return RedirectDao.class;
        }

        @Override
        public String getPath() {
            return "dao.redirect";
        }

        @Override
        public RedirectSqlDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")){
                return null;
            }
            try {
                return new RedirectSqlDao(
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
