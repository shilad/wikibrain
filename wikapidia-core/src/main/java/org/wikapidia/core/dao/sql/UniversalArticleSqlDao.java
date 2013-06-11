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
import org.wikapidia.core.dao.UniversalArticleDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalArticle;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

/**
 */
public class UniversalArticleSqlDao extends UniversalPageSqlDao<UniversalArticle> implements UniversalArticleDao {

    public UniversalArticleSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public UniversalArticle getById(int univId) throws DaoException {
        return super.getById(univId, PageType.ARTICLE);
    }

    @Override
    public Map<Integer, UniversalArticle> getByIds(Collection<Integer> univIds) throws DaoException {
        return super.getByIds(univIds, PageType.ARTICLE);
    }

    @Override
    protected UniversalArticle buildUniversalPage(Result<Record> result) throws DaoException {
        if (result == null) {
            return null;
        }
        Multimap<Language, LocalArticle> localPages = HashMultimap.create(result.size(), result.size());
        for(Record record : result) {
            PageType pageType = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
            if (pageType != PageType.ARTICLE) {
                throw new DaoException("Tried to get ARTICLE, but found " + pageType);
            }
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            Title title = new Title(
                    record.getValue(Tables.UNIVERSAL_PAGE.TITLE), true,
                    LanguageInfo.getByLanguage(language));
            localPages.put(language,
                    new LocalArticle(
                            language,
                            record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID),
                            title
                    )
            );
        }
        return new UniversalArticle(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                localPages
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalArticleDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalArticleDao.class;
        }

        @Override
        public String getPath() {
            return "dao.universalArticle";
        }

        @Override
        public UniversalArticleDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new UniversalArticleSqlDao(
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
