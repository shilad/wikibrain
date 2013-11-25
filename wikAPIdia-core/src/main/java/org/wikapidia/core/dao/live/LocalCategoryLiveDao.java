package org.wikapidia.core.dao.live;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.util.Collection;
import java.util.Map;

/**
 * A Live Wiki API Implementation of LocalCategoryDao
 * @author Toby "Jiajun" Li
 */
public class LocalCategoryLiveDao extends LocalPageLiveDao<LocalCategory> implements LocalCategoryDao {

    public LocalCategoryLiveDao() throws DaoException{
        super();
    }

    /**
     * Returns a LocalCategory based on language and title, with namespace assumed as CATEGORY.
     *
     * @param language the language of the category
     * @param title the title of the category to be searched for
     * @return a LocalCategory object
     * @throws DaoException
     */

    public LocalCategory getByTitle(Language language, Title title) throws DaoException{
        return new LocalCategory(super.getByTitle(title, NameSpace.CATEGORY));
    }

    /**
     * Returns a Map of LocalCategories based on language and a collection of titles, with namespace assumed as CATEGORY.
     *
     * @param language the language of the categories
     * @param titles the titles to be searched for
     * @return a Map of LocalCategories mapped to their titles
     * @throws DaoException
     */

    public Map<Title, LocalCategory> getByTitles(Language language, Collection<Title> titles) throws DaoException{
        return super.getByTitles(language, titles, NameSpace.CATEGORY);
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
            if (!config.getString("type").equals("live")) {
                return null;
            }
            try {
                return new LocalCategoryLiveDao();

            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
