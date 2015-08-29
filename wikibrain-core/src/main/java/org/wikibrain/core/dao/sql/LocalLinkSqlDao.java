package org.wikibrain.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


public class LocalLinkSqlDao extends AbstractSqlDao<LocalLink> implements LocalLinkDao {

    public LocalLinkSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/local-link");
    }

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.LOCAL_LINK.LANG_ID,
            Tables.LOCAL_LINK.ANCHOR_TEXT,
            Tables.LOCAL_LINK.SOURCE_ID,
            Tables.LOCAL_LINK.DEST_ID,
            Tables.LOCAL_LINK.LOCATION,
            Tables.LOCAL_LINK.IS_PARSEABLE,
            Tables.LOCAL_LINK.LOCATION_TYPE,
    };

    @Override
    public void save(LocalLink localLink) throws DaoException {
        insert(
            localLink.getLanguage().getId(),
            localLink.getAnchorText(),
            localLink.getSourceId(),
            localLink.getDestId(),
            localLink.getLocation(),
            localLink.isParseable(),
            localLink.getLocType().ordinal()
        );
    }

    @Override
    public Iterable<LocalLink> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.LOCAL_LINK.LANG_ID.in(daoFilter.getLangIds()));
            }
            if (daoFilter.getLocTypes() != null) {
                conditions.add(Tables.LOCAL_LINK.LOCATION_TYPE.in(daoFilter.getLocTypes()));
            }
            if (daoFilter.getSourceIds() != null) {
                conditions.add(Tables.LOCAL_LINK.SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getDestIds() != null) {
                conditions.add(Tables.LOCAL_LINK.DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isParseable() != null) {
                conditions.add(Tables.LOCAL_LINK.IS_PARSEABLE.in(daoFilter.isParseable()));
            }
            if (daoFilter.getHasDest() != null) {
                conditions.add(Tables.LOCAL_LINK.DEST_ID.ne(-1));
            }
            Cursor<Record> result = context.select().
                    from(Tables.LOCAL_LINK).
                    where(conditions).
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<LocalLink>(result, context) {
                @Override
                public LocalLink transform(Record r) {
                    return buildLocalLink(r, true);
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
                conditions.add(Tables.LOCAL_LINK.LANG_ID.in(daoFilter.getLangIds()));
            }
            if (daoFilter.getLocTypes() != null) {
                conditions.add(Tables.LOCAL_LINK.LOCATION_TYPE.in(daoFilter.getLocTypes()));
            }
            if (daoFilter.getSourceIds() != null) {
                conditions.add(Tables.LOCAL_LINK.SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getDestIds() != null) {
                conditions.add(Tables.LOCAL_LINK.DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isParseable() != null) {
                conditions.add(Tables.LOCAL_LINK.IS_PARSEABLE.in(daoFilter.isParseable()));
            }
            if (daoFilter.getHasDest() != null) {
                conditions.add(Tables.LOCAL_LINK.DEST_ID.ne(-1));
            }
            return context.selectDistinct(Tables.LOCAL_LINK.SOURCE_ID,Tables.LOCAL_LINK.DEST_ID).
                    from(Tables.LOCAL_LINK).
                    where(conditions).
                    fetchCount();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public LocalLink getLink(Language language, int sourceId, int destId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().
                    from(Tables.LOCAL_LINK).
                    where(Tables.LOCAL_LINK.LANG_ID.eq(language.getId())).
                    and(Tables.LOCAL_LINK.SOURCE_ID.eq(sourceId)).
                    and(Tables.LOCAL_LINK.DEST_ID.eq(destId)).
                    fetch();
            //Work-around to avoid pages that have multiple links to the same page
            Record record;
            if (result.isEmpty()){
                record = null;
            }
            else {
                record = result.get(0);
            }
            return buildLocalLink(record, true);
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    @Override
    public double getPageRank(Language language, int pageId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getPageRank(LocalId localId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks, boolean isParseable, LocalLink.LocationType locationType) throws DaoException{
        DSLContext context = getJooq();
        try {
            TableField idField;
            if (outlinks){
                idField = Tables.LOCAL_LINK.SOURCE_ID;
            } else {
                idField = Tables.LOCAL_LINK.DEST_ID;
            }
            Cursor<Record> result = context.select()
                    .from(Tables.LOCAL_LINK)
                    .where(Tables.LOCAL_LINK.LANG_ID.equal(language.getId()))
                    .and(idField.equal(localId))
                    .and(Tables.LOCAL_LINK.IS_PARSEABLE.equal(isParseable))
                    .and(Tables.LOCAL_LINK.LOCATION_TYPE.equal((short) locationType.ordinal()))
                    .fetchLazy(getFetchSize());
            return buildLocalLinks(result, outlinks, context);
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

//    private static final AtomicLong counter = new AtomicLong();
//    private static final AtomicLong timer = new AtomicLong();
    @Override
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks) throws DaoException{
//        if (counter.incrementAndGet() % 1000 == 0) {
//            double mean = 1.0 * timer.get() / counter.get();
//            System.out.println("counter is " + counter.get() + ", mean millis is " + mean);
//        }
        DSLContext context = getJooq();
//        long start = System.currentTimeMillis();
        try {
            TableField idField;
            if (outlinks){
                idField = Tables.LOCAL_LINK.SOURCE_ID;
            } else {
                idField = Tables.LOCAL_LINK.DEST_ID;
            }
            Cursor<Record> result = context.select()
                    .from(Tables.LOCAL_LINK)
                    .where(Tables.LOCAL_LINK.LANG_ID.equal(language.getId()))
                    .and(idField.equal(localId))
                    .fetchLazy(getFetchSize());
//            long end = System.currentTimeMillis();
//            timer.addAndGet(end - start);
            return buildLocalLinks(result, outlinks, context);
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    private Iterable<LocalLink> buildLocalLinks(Cursor<Record> result, final boolean outlink, DSLContext context){
        return new SimpleSqlDaoIterable<LocalLink>(result, context) {
            @Override
            public LocalLink transform(Record r) {
                return buildLocalLink(r, outlink);
            }
        };
    }

    private LocalLink buildLocalLink(Record record, boolean outlink){
        if (record == null){
            return null;
        }
        return new LocalLink(
                Language.getById(record.getValue(Tables.LOCAL_LINK.LANG_ID)),
                record.getValue(Tables.LOCAL_LINK.ANCHOR_TEXT),
                record.getValue(Tables.LOCAL_LINK.SOURCE_ID),
                record.getValue(Tables.LOCAL_LINK.DEST_ID),
                outlink,
                record.getValue(Tables.LOCAL_LINK.LOCATION),
                record.getValue(Tables.LOCAL_LINK.IS_PARSEABLE),
                LocalLink.LocationType.values()[record.getValue(Tables.LOCAL_LINK.LOCATION_TYPE)]
        );
    }

    public static class Provider extends org.wikibrain.conf.Provider<LocalLinkDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<LocalLinkDao> getType() {
            return LocalLinkDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localLink";
        }

        @Override
        public LocalLinkSqlDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalLinkSqlDao(
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
