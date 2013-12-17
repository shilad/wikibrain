package org.wikapidia.core.dao.live;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.LocalPage;

import org.wikapidia.core.model.Title;

import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: Toby "Jiajun" Li
 * Date: 10/26/13
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
 */


/**
 * Fetch a LocalPage object from Wikipedia Server
 * @param <T> LocalPage object fetched
 */

public class LocalPageLiveDao<T extends LocalPage> implements LocalPageDao<T> {
    /**
     * Sets if we should try to follow the redirects or not. Default is true (to following them).
     * @param followRedirects
     */

    private boolean followRedirects = true;

    public LocalPageLiveDao() throws DaoException {
    }

    //Notice: A DaoException will be thrown if you call the methods below!
    public void clear()throws DaoException{
       throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public void beginLoad()throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public void endLoad()throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public void save(T a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public int getCount(DaoFilter a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public Iterable<T> get(DaoFilter a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public LanguageSet getLoadedLanguages() throws DaoException {
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");

    }

    public void setFollowRedirects(boolean followRedirects) throws DaoException {
        this.followRedirects = followRedirects;
    }

    /**
     * Get a single page by its title
     *
     * @param title the page's title
     * @param ns the page's namespace
     * @return the requested LocalPage
     * @throws org.wikapidia.core.dao.DaoException if there was an error retrieving the page
     */
    public T getByTitle(Title title, NameSpace ns) throws DaoException{
        LocalPageQueryReply info = new LocalPageQueryReply(LiveUtils.getInfoByQuery(getQueryByTitle(title)));
        return (T)new LocalPage(title.getLanguage(), info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig());
    }

    /**
     * Get a single page by its title
     * @param language the page's language
     * @param pageId the page's id
     * @return the requested LocalPage
     * @throws org.wikapidia.core.dao.DaoException if there was an error retrieving the page
     */
    public T getById(Language language, int pageId) throws DaoException{
        LocalPageQueryReply info = new LocalPageQueryReply(LiveUtils.getInfoByQuery(getQueryByID(pageId, language)));
        return (T)new LocalPage(language, info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig());
    }

    /**
     * Get a set of pages by their ids
     * @param language the language of the pages
     * @param pageIds a Collection of page ids
     * @return a map of ids to pages
     * @throws org.wikapidia.core.dao.DaoException if there was an error retrieving the pages
     */
    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) throws DaoException{
        Map<Integer,T> pageMap = new HashMap<Integer, T>();
        for(Integer pageId : pageIds){
            LocalPageQueryReply info = new LocalPageQueryReply(LiveUtils.getInfoByQuery(getQueryByID(pageId, language)));
            pageMap.put(pageId, (T)new LocalPage(language, info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig()));
        }
        return pageMap;
    }

    /**
     * Get a map of pages by their titles
     * @param language the language of the pages
     * @param titles a Collection of page titles
     * @param ns the namespace of the pages
     * @return a map of titles to pages
     * @throws org.wikapidia.core.dao.DaoException if there was an error retrieving the pages
     */
    public Map<Title, T> getByTitles(Language language, Collection<Title> titles, NameSpace ns) throws DaoException{
        Map<Title, T> pageMap = new HashMap<Title, T>();
        for(Title title : titles){
            LocalPageQueryReply info = new LocalPageQueryReply(LiveUtils.getInfoByQuery(getQueryByTitle(title)));
            pageMap.put(title, (T)new LocalPage(language, info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig()));
        }
        return pageMap;
    }


    /**
     * Get an id from a title. Returns -1 if it doesn't exist.
     * @param title
     * @param language
     * @param nameSpace
     * @return
     */
    public int getIdByTitle(String title, Language language, NameSpace nameSpace) throws DaoException{
        LocalPageQueryReply info = new LocalPageQueryReply(LiveUtils.getInfoByQuery(getQueryByTitle(new Title(title, language))));
        return info.getId();
    }

    /**
     * Get an id from a title. Returns -1 if it doesn't exist.
     * @param title
     * @return
     */
    public int getIdByTitle(Title title) throws DaoException{
        LocalPageQueryReply info = new LocalPageQueryReply(LiveUtils.getInfoByQuery(getQueryByTitle(title)));
        return info.getId();
    }

    private String getQueryByTitle(Title title){
        Language language = title.getLanguage();
        String http = new String("http://");
        String host = new String(".wikipedia.org");
        String query = new String("/w/api.php?action=query&prop=info&format=json&titles=");
        if(followRedirects)
            return http + language.getLangCode() + host + query + title.getCanonicalTitle().replaceAll(" ", "_") + "&redirects=";
        else
            return http + language.getLangCode() + host + query + title.getCanonicalTitle().replaceAll(" ", "_");

    }

    private String getQueryByID(Integer pageId, Language language){
        String http = new String("http://");
        String host = new String(".wikipedia.org");
        String query = new String("/w/api.php?action=query&prop=info&format=json&pageids=");
        if(followRedirects)
            return http + language.getLangCode() + host + query + pageId.toString();
        else
            return http + language.getLangCode() + host + query + pageId.toString() + "&redirects=";
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalPageDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalPageDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localPage";
        }

        @Override
        public LocalPageDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("live")) {
                return null;
            }
            try {
                return new LocalPageLiveDao();

            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}

