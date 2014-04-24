package org.wikibrain.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.Record;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalArticleDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalArticle;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;

import java.util.Collection;
import java.util.Map;

public class LocalArticleSqlDao extends LocalPageSqlDao<LocalArticle> implements LocalArticleDao{

    public LocalArticleSqlDao(WpDataSource dataSource) throws DaoException {
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
        return super.getByTitle(title, NameSpace.ARTICLE);
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

    public static class Provider extends org.wikibrain.conf.Provider<LocalArticleDao> {
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
        public LocalArticleDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalArticleSqlDao(
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
