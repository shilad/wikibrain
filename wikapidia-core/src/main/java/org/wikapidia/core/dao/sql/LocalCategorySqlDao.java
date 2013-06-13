package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.Record;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

public class LocalCategorySqlDao extends LocalPageSqlDao<LocalCategory> implements LocalCategoryDao {

    public LocalCategorySqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    /**
     * Returns a LocalCategory based on language and title, with namespace assumed as CATEGORY.
     *
     * @param language the language of the category
     * @param title the title of the category to be searched for
     * @return a LocalCategory object
     * @throws DaoException
     */
    @Override
    public LocalCategory getByTitle(Language language, Title title) throws DaoException {
        return super.getByTitle(language, title, NameSpace.CATEGORY);
    }

    /**
     * Returns a Map of LocalCategories based on language and a collection of titles, with namespace assumed as CATEGORY.
     *
     * @param language the language of the categories
     * @param titles the titles to be searched for
     * @return a Map of LocalCategories mapped to their titles
     * @throws DaoException
     */
    @Override
    public Map<Title, LocalCategory> getByTitles(Language language, Collection<Title> titles) throws DaoException{
        return super.getByTitles(language, titles, NameSpace.CATEGORY);
    }


    @Override
    protected LocalCategory buildLocalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        NameSpace nameSpace = NameSpace.getNameSpaceById(record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        if (nameSpace != NameSpace.CATEGORY) {
            throw new DaoException("Tried to get CATEGORY, but found " + nameSpace);
        }
        return new LocalCategory(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalCategoryDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalCategoryDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localCategory";
        }

        @Override
        public LocalCategoryDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalCategorySqlDao(
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
