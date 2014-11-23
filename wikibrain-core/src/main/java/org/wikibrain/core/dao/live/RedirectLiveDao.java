package org.wikibrain.core.dao.live;

import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.RedirectDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/9/13
 * Time: 4:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class RedirectLiveDao implements RedirectDao {

    public RedirectLiveDao() throws DaoException {}

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
    public void save(Redirect a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public int getCount(DaoFilter a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public Iterable<Redirect> get(DaoFilter a)throws DaoException{
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }
    public LanguageSet getLoadedLanguages() throws DaoException {
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }

    public void save(Language lang, int src, int dest) throws DaoException {
        throw new UnsupportedOperationException("Can't use this method for remote wiki server!");
    }

    public Integer resolveRedirect(Language lang, int id) throws DaoException {
        //get pageid of page that id redirects to
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("INFO", lang);
        builder.addPageid(id).setRedirects(true);
        LiveAPIQuery query = builder.build();
        int redirectId = query.getValuesFromQueryResult().get(0).pageId;
        if (redirectId != id) {
            return redirectId;
        }
        return null; //if the redirect id was the same as the input id, id wasn't a redirect page
    }

    public boolean isRedirect(Language lang, int id) throws DaoException {
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("INFO", lang);
        builder.addPageid(id);
        LiveAPIQuery query = builder.build();
        return query.getValuesFromQueryResult().get(0).isRedirect;
    }

    public TIntSet getRedirects(LocalPage localPage) throws DaoException {
        List<Integer> redirects = getRedirectsFromId(localPage.getLanguage(), localPage.getLocalId());
        return new TIntHashSet(redirects);
    }


    public List<Integer> getRedirectsFromId(Language lang, int localId) throws DaoException {
        List<Integer> redirectIds = new ArrayList<Integer>();
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("BACKLINKS", lang);
        builder.addPageid(localId).setFilterredir("redirects");
        LiveAPIQuery query = builder.build();
        List<QueryReply> replyObjects = query.getValuesFromQueryResult();
        for (QueryReply reply : replyObjects) {
            redirectIds.add(reply.pageId);
        }
        return redirectIds;
    }

    public TIntIntMap getAllRedirectIdsToDestIds(Language lang) throws DaoException {
        TIntIntMap redirects = new TIntIntHashMap();
        LiveAPIQuery.LiveAPIQueryBuilder builder = new LiveAPIQuery.LiveAPIQueryBuilder("ALLPAGES", lang);
        builder.setFilterredir("redirects").setFrom("");
        LiveAPIQuery query = builder.build();
        List<QueryReply> replyObjects = query.getValuesFromQueryResult();

        for (QueryReply reply : replyObjects) {
            int destId = resolveRedirect(lang, reply.pageId);
            redirects.put(reply.pageId, destId);
        }

        return  redirects;
    }

    public static class Provider extends org.wikibrain.conf.Provider<RedirectDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return RedirectDao.class;
        }

        @Override
        public String getPath() {
            return "dao.redirect";
        }

        @Override
        public RedirectDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("live")) {
                return null;
            }
            try {
                return new RedirectLiveDao();

            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
