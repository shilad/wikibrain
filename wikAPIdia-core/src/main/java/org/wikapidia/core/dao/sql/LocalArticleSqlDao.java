package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.Record;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

public class LocalArticleSqlDao extends LocalPageSqlDao<LocalArticle> implements LocalArticleDao{

    public LocalArticleSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    /**
     * Returns a LocalArticle based on language and title, with namespace assumed as ARTICLE.
     *
     * @param language the language of the article
     * @param title the title of the article to be searched for
     * @return a LocalArticle object
     * @throws DaoException
     */
    public LocalArticle getByTitle(Language language, Title title) throws DaoException {
        return super.getByTitle(language, title, NameSpace.ARTICLE);
    }

    /**
     * Returns a Map of LocalCategories based on language and a collection of titles, with namespace assumed as ARTICLE.
     *
     * @param language the language of the categories
     * @param titles the titles to be searched for
     * @return a Map of LocalCategories mapped to their titles
     * @throws DaoException
     */
    public Map<Title, LocalArticle> getByTitles(Language language, Collection<Title> titles) throws DaoException{
        return super.getByTitles(language, titles, NameSpace.ARTICLE);
    }

    @Override
    protected LocalArticle buildLocalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        NameSpace nameSpace = NameSpace.getNameSpaceByArbitraryId(record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        if (nameSpace != NameSpace.ARTICLE) {
            throw new DaoException("Tried to get ARTICLE, but found " + nameSpace);
        }
        return new LocalArticle(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title,
                record.getValue(Tables.LOCAL_PAGE.IS_REDIRECT),
                record.getValue(Tables.LOCAL_PAGE.IS_DISAMBIG)
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalArticleDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalArticleDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localArticle";
        }

        @Override
        public LocalArticleDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalArticleSqlDao(
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
