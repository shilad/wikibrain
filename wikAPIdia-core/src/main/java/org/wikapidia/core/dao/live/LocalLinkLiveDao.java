package org.wikapidia.core.dao.live;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
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
        //get list of pageids and titles of all outlinks from sourceId
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("&generator=links&pageids=" + sourceId, language, "pages", false);
        LiveAPIQuery query = builder.build();
        List<String> linkTitles = query.getStringsFromQueryResult("title");
        List<Integer> linkPageIds = query.getIntsFromQueryResult("pageid");
        
        //check all outlinks from sourceId to find one that matches destId
        for (int i = 0; i < linkPageIds.size(); i++) {
            int pageId = linkPageIds.get(i);
            if (pageId == destId) {
                return new LocalLink(language, linkTitles.get(i), sourceId, pageId, true, -1, true, null);
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
        List<LocalLink> links = new ArrayList<LocalLink>();
        String queryArgs = outlinks ? "&generator=links&pageids=" + localId : "&list=backlinks&blpageid=" + localId;
        String linkType = outlinks ? "pages" : "backlinks";
        
        /*inlink information is returned as an array, but outlink information is returned as a JSON object
         *so parseArray in LiveAPIQuery is true iff "outlinks" is false*/
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder(queryArgs, language, linkType, !outlinks);
        LiveAPIQuery query = builder.build();
        //query for outlinks from local id, return as list of titles and pageids
        List<String> linkTitles = query.getStringsFromQueryResult("title");
        List<Integer> linkPageIds = query.getIntsFromQueryResult("pageid");

        //create a link for each title and pageid returned from the query
        for (int i = 0; i < linkTitles.size(); i++) {
            String anchorText = linkTitles.get(i);
            Integer pageId = linkPageIds.get(i);
            LocalLink link = new LocalLink(language, anchorText, localId, pageId, outlinks, -1, true, null);
            links.add(link);
        }

        return links;
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
        public LocalLinkDao get(String name, Config config) throws ConfigurationException {
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


