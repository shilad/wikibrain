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
import org.wikibrain.core.dao.InterLanguageLinkDao;
import org.wikibrain.core.dao.RedirectDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Redirect;

import java.util.*;

/**
 */
public class InterLanguageLinkSqlDao extends AbstractSqlDao<InterLanguageLink> implements InterLanguageLinkDao {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.ILL.SOURCE_LANG_ID,
            Tables.ILL.SOURCE_ID,
            Tables.ILL.DEST_LANG_ID,
            Tables.ILL.DEST_ID,
    };

    public InterLanguageLinkSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/ill");
    }

    @Override
    public void save(InterLanguageLink ill) throws DaoException {
        insert(
                ill.getSource().getLanguage().getId(),
                ill.getSource().getId(),
                ill.getDest().getLanguage().getId(),
                ill.getDest().getId()
        );
    }

    /**
     * Generally this method should not be used.
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    @Override
    public Iterable<InterLanguageLink> get(DaoFilter daoFilter) throws DaoException {
        throw new UnsupportedOperationException();
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
    public Set<LocalId> getFromSource(Language sourceLang, int sourceId) throws DaoException {
        return getFromSource(new LocalId(sourceLang, sourceId));
    }

    @Override
    public Set<LocalId> getFromSource(LocalId src) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().from(Tables.ILL)
                    .where(Tables.ILL.SOURCE_LANG_ID.equal(src.getLanguage().getId()))
                    .and(Tables.ILL.SOURCE_ID.equal(src.getId()))
                    .fetch();
            if (result == null){
                return null;
            }
            Set<LocalId> ills = new HashSet<LocalId>();
            for (Record record : result) {
                ills.add(new LocalId(
                        Language.getById(record.getValue(Tables.ILL.DEST_LANG_ID)),
                        record.getValue(Tables.ILL.DEST_ID)));
            }
            return ills;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Set<LocalId> getToDest(Language destLang, int destId) throws DaoException {
        return getToDest(new LocalId(destLang, destId));
    }

    @Override
    public Set<LocalId> getToDest(LocalId dest) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().from(Tables.ILL)
                    .where(Tables.ILL.DEST_LANG_ID.equal(dest.getLanguage().getId()))
                    .and(Tables.ILL.DEST_ID.equal(dest.getId()))
                    .fetch();
            if (result == null){
                return null;
            }
            Set<LocalId> ills = new HashSet<LocalId>();
            for (Record record : result) {
                ills.add(new LocalId(
                        Language.getById(record.getValue(Tables.ILL.SOURCE_LANG_ID)),
                        record.getValue(Tables.ILL.SOURCE_ID)));
            }
            return ills;
        } finally {
            freeJooq(context);
        }
    }

    private InterLanguageLink buildIll(Record r) {
        if (r == null){
            return null;
        }
        return new InterLanguageLink(
                Language.getById(r.getValue(Tables.ILL.SOURCE_LANG_ID)),
                r.getValue(Tables.ILL.SOURCE_ID),
                Language.getById(r.getValue(Tables.ILL.SOURCE_LANG_ID)),
                r.getValue(Tables.ILL.SOURCE_ID)
        );
    }

    public static class Provider extends org.wikibrain.conf.Provider<InterLanguageLinkSqlDao>  {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return InterLanguageLinkDao.class;
        }

        @Override
        public String getPath() {
            return "dao.interLanguageLink";
        }

        @Override
        public InterLanguageLinkSqlDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")){
                return null;
            }
            try {
                return new InterLanguageLinkSqlDao(
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
