package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.UniversalPage;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class UniversalPageSqlDao<T extends UniversalPage> extends AbstractSqlDao implements UniversalPageDao<T> {

    public UniversalPageSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            UniversalPageSqlDao.class.getResource("/db/universal-page-schema.sql")
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
    public void save(UniversalPage page) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            UniversalPage<LocalPage> temp = page;
            NameSpace nameSpace = temp.getNameSpace();
            for (Language language : temp.getLanguageSetOfExistsInLangs()) {
                for (LocalPage localPage : temp.getLocalPages(language)) {
                    context.insertInto(Tables.UNIVERSAL_PAGE).values(
                            language.getId(),
                            localPage.getLocalId(),
                            nameSpace.getArbitraryId(),
                            page.getUnivId(),
                            page.getAlgorithmId()
                    ).execute();
                }
            }
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
                            UniversalPageSqlDao.class.getResource("/db/universal-page-indexes.sql")
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
    public T getById(int univId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select().
                    from(Tables.UNIVERSAL_PAGE).
                    where(Tables.UNIVERSAL_PAGE.UNIV_ID.eq(univId)).
                    and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId)).
                    fetch();
            return (T)buildUniversalPage(result);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public Map<Integer, T> getByIds(Collection<Integer> univIds, int algorithmId) throws DaoException {
        if (univIds == null || univIds.isEmpty()) {
            return null;
        }
        Map<Integer, T> map = new HashMap<Integer, T>();
        for (Integer univId : univIds){
            map.put(univId, getById(univId, algorithmId));
        }
        return map;
    }

    /**
     * Build a UniversalPage from a database record representation.
     * Classes that extend class this should override this method.
     *
     * @param result a list of database records
     * @return a UniversalPage representation of the given database record
     * @throws DaoException if the record is not a Page
     */
    protected UniversalPage buildUniversalPage(Result<Record> result) throws DaoException {
        if (result == null) {
            return null;
        }
        Multimap<Language, LocalPage> localPages = HashMultimap.create(result.size(), result.size());
        NameSpace nameSpace = NameSpace.getNameSpaceById(result.get(0).getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        LocalPageSqlDao localDao = new LocalPageSqlDao(ds);
        for(Record record : result) {
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            int pageId = record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID);
            localPages.put(language, localDao.getById(language, pageId));
        }
        return new UniversalPage<LocalPage>(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID),
                nameSpace,
                localPages
        ){};
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalPageDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalPageDao.class;
        }

        @Override
        public String getPath() {
            return "dao.universalPage";
        }

        @Override
        public UniversalPageDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new UniversalPageSqlDao(
                        (DataSource) getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
