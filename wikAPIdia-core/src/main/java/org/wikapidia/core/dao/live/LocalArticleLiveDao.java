package org.wikapidia.core.dao.live;


import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Toby "Jiajun" Li
 * Date: 11/3/13
 * Time: 1:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalArticleLiveDao extends LocalPageLiveDao<LocalArticle> implements LocalArticleDao {

    public LocalArticleLiveDao() throws DaoException {
        super();
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
            if (!config.getString("type").equals("live")) {
                return null;
            }
            try{
                return new LocalArticleLiveDao();
            }
            catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
