package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.jooq.Record;
import org.jooq.Result;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalCategoryDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalCategory;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

/**
 */
public class UniversalCategorySqlDao extends UniversalPageSqlDao<UniversalCategory> implements UniversalCategoryDao {

    public UniversalCategorySqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public UniversalCategory getById(int univId) throws DaoException {
        return super.getById(univId, PageType.CATEGORY);
    }

    @Override
    public Map<Integer, UniversalCategory> getByIds(Collection<Integer> univIds) throws DaoException {
        return super.getByIds(univIds, PageType.CATEGORY);
    }

    @Override
    protected UniversalCategory buildUniversalPage(Result<Record> result) throws DaoException {
        if (result == null) {
            return null;
        }
        Multimap<Language, LocalCategory> localPages = HashMultimap.create(result.size(), result.size());
        for(Record record : result) {
            PageType pageType = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
            if (pageType != PageType.ARTICLE) {
                throw new DaoException("Tried to get CATEGORY, but found " + pageType);
            }
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            Title title = new Title(
                    record.getValue(Tables.UNIVERSAL_PAGE.TITLE), true,
                    LanguageInfo.getByLanguage(language));
            localPages.put(language,
                    new LocalCategory(
                            language,
                            record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID),
                            title
                    )
            );
        }
        return new UniversalCategory(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
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
