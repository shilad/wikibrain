package org.wikibrain.core.dao.live;


import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.Title;

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
        if(a.getSourceIds() == null && a.getDestIds() == null)
            throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
        else{
            int sum=0;
            Iterator<LocalLink> it = get(a).iterator();
            while (it.hasNext())
            {
                it.next();
                sum++;
            }
            return sum;
        }
    }
    public Iterable<LocalLink> get(DaoFilter a)throws DaoException{
        if(a.getSourceIds() == null && a.getDestIds() == null)
            throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
        else if (a.getSourceIds() != null && a.getDestIds() == null){
            Set<LocalLink> set = new HashSet<LocalLink>();
            for (short langId : a.getLangIds()){
                for (int srcId : a.getSourceIds()){
                    for(LocalLink link: getLinks(Language.getById(langId), srcId, true))
                        set.add(link);
                }
            }
            return set;
        }
        else if (a.getSourceIds() == null && a.getDestIds() != null){
            Set<LocalLink> set = new HashSet<LocalLink>();
            for (short langId : a.getLangIds()){
                for (int dstId : a.getDestIds()){
                    for(LocalLink link: getLinks(Language.getById(langId), dstId, false))
                        set.add(link);
                }
            }
            return set;
        }
        else{
            Set<LocalLink> inSet = new HashSet<LocalLink>();
            for (short langId : a.getLangIds()){
                for (int srcId : a.getSourceIds()){
                    for(LocalLink link: getLinks(Language.getById(langId), srcId, true))
                        inSet.add(link);
                }
            }
            Set<LocalLink> outSet = new HashSet<LocalLink>();
            for (short langId : a.getLangIds()){
                for (int dstId : a.getDestIds()){
                    for(LocalLink link: getLinks(Language.getById(langId), dstId, false))
                        outSet.add(link);
                }
            }
            Set<LocalLink> interSec = new HashSet<LocalLink>();
            for (LocalLink link: inSet){
                if (outSet.contains(link))
                    interSec.add(link);
            }
            return interSec;
        }

    }
    public LanguageSet getLoadedLanguages() throws DaoException {
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    
    public LocalLink getLink(Language language, int sourceId, int destId) throws DaoException {
        //get list of pageids and titles of all outlinks from sourceId
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("LINKS", language);
        builder.addPageid(sourceId);
        LiveAPIQuery query = builder.build();
        List<QueryReply> replyObjects = query.getValuesFromQueryResult();

        //check all outlinks from sourceId to find one that matches destId
        for (QueryReply reply : replyObjects) {
            int pageId = reply.pageId;
            if (pageId == destId) {
                return reply.getLocalOutLink(language, sourceId);
            }
        }
        throw new DaoException("No link with given sourceId and destId found");
    }

    @Override
    public double getPageRank(Language language, int pageId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getPageRank(LocalId localId) {
        throw new UnsupportedOperationException();
    }

    //Notice: A DaoException will be thrown if you call this method!
    //Can't specify isParseable or LocationType through the live API
    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks, boolean isParseable, LocalLink.LocationType locationType) throws DaoException {
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }

    public Iterable<LocalLink> getLinks(Language language, int localId, boolean outlinks) throws DaoException {
        List<LocalLink> links = new ArrayList<LocalLink>();
        LiveAPIQuery.LiveAPIQueryBuilder builder;
        if (outlinks) {
            builder = new LiveAPIQuery.LiveAPIQueryBuilder("LINKS", language);
        }
        else {
            builder = new LiveAPIQuery.LiveAPIQueryBuilder("BACKLINKS", language);
        }
        builder.addPageid(localId);
        LiveAPIQuery query = builder.build();

        //query for outlinks from local id, return as list of titles and pageids
        List<QueryReply> replyObjects = query.getValuesFromQueryResult();
        for (QueryReply reply : replyObjects) {
            LocalLink link = outlinks ? reply.getLocalOutLink(language, localId) : reply.getLocalInLink(language, localId);
            links.add(link);
        }

        return links;
    }

    public static class Provider extends org.wikibrain.conf.Provider<LocalLinkDao> {
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


