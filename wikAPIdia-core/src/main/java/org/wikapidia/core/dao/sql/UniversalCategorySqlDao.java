package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.jooq.Record;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalCategoryDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.UniversalCategory;

import javax.sql.DataSource;
import java.util.List;

/**
 *
 * @author Ari Weiland
 *
 */
public class UniversalCategorySqlDao extends UniversalPageSqlDao<UniversalCategory> implements UniversalCategoryDao {

    public UniversalCategorySqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    protected UniversalCategory buildUniversalPage(List<Record> result) throws DaoException {
        if (result.isEmpty()) {
            return null;
        }
        Multimap<Language, LocalId> localPages = HashMultimap.create(result.size(), result.size());
        for(Record record : result) {
            NameSpace nameSpace = NameSpace.getNameSpaceByArbitraryId(record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
            if (nameSpace != NameSpace.CATEGORY) {
                throw new DaoException("Tried to get CATEGORY, but found " + nameSpace);
            }
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            int pageId = record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID);
            LocalCategorySqlDao localDao = new LocalCategorySqlDao(ds);
            localPages.put(language, new LocalId(language,pageId));
        }
        return new UniversalCategory(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID),
                localPages
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalCategoryDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalCategoryDao.class;
        }

        @Override
        public String getPath() {
            return "dao.universalCategory";
        }

        @Override
        public UniversalCategoryDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new UniversalCategorySqlDao(
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
