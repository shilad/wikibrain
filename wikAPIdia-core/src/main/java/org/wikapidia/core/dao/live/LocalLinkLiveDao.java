package org.wikapidia.core.dao.live;


import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalLink;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/1/13
 * Time: 11:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalLinkLiveDao implements LocalLinkDao {

    public LocalLinkLiveDao() throws DaoException {}

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
    public void save(LocalLink a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public int getCount(DaoFilter a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public Iterable<LocalLink> get(DaoFilter a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public LanguageSet getLoadedLanguages() throws DaoException {
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    
    public LocalLink getLink(Language language, int sourceId, int destId) throws DaoException {
        Iterable<LocalLink> links = LocalLinkLiveUtils.parseLinks(getLinkJson(language, sourceId, true), language, sourceId, true);
        for (LocalLink link : links) {
            if (link.getDestId() == destId) {
                return link;
            }
        }
        throw new DaoException("No link with given sourceId and destId found");
    }

    //Notice: A DaoException will be thrown if you call this method!
    //Can't specify isParseable or LocationType through the live API
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks, boolean isParseable, LocalLink.LocationType locationType) throws DaoException {
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }

    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks) throws DaoException {
        return LocalLinkLiveUtils.parseLinks(getLinkJson(language, localId, outlinks), language, localId, outlinks);
    }

    /**
     * Query the wikipedia server for links from or to a specific page, specified by sourceId
     * Returns JSON results of the query
     * @param language
     * @param sourceId
     * @param outlinks
     * @return
     * @throws DaoException
     */
    private String getLinkJson(Language language, int sourceId, boolean outlinks) throws DaoException {
        String http = "http://";
        String host = ".wikipedia.org";
        String prop = outlinks ? "generator=links" : "list=backlinks";
        String pageIdRequest = outlinks ? "pageids=" + sourceId : "blpageid=" + sourceId;
        String query = http + language.getLangCode() + host + "/w/api.php?action=query&" + prop + "&format=json&" + pageIdRequest;
        return LiveUtils.getInfoByQuery(query);
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalLinkDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalLinkDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localLink";
        }

        @Override
        public LocalLinkDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("live")) {
                return null;
            }
            try {
                return new LocalLinkLiveDao();

            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}


