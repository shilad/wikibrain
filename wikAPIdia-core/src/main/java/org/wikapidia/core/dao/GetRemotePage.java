package org.wikapidia.core.dao;

import com.google.gson.JsonElement;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RemotePage;
import org.wikapidia.core.model.Title;

import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



/**
 * Created with IntelliJ IDEA.
 * User: toby
 * Date: 10/26/13
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetRemotePage {

    /**
     * Sets if we should try to follow the redirects or not. Default is true (to following them).
     * @param followRedirects
     */

    private boolean followRedirects = true;



    public void setFollowRedirects(boolean followRedirects) throws DaoException{
        this.followRedirects = followRedirects;
    }

    /**
     * Get a single page by its title
     * @param language the page's language
     * @param title the page's title
     * @param ns the page's namespace
     * @return the requested LocalPage
     * @throws org.wikapidia.core.dao.DaoException if there was an error retrieving the page
     */
    public RemotePage getByTitle(Language language, Title title, NameSpace ns) throws DaoException{
        QueryType info = getPageInfo(getInfoByQuery(getQueryByTitle(title, language)));
        return new RemotePage(language, info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig());
    }

    /**
     * Get a single page by its title
     * @param language the page's language
     * @param pageId the page's id
     * @return the requested LocalPage
     * @throws org.wikapidia.core.dao.DaoException if there was an error retrieving the page
     */
    public RemotePage getById(Language language, int pageId) throws DaoException{
        QueryType info = getPageInfo(getInfoByQuery(getQueryByID(pageId, language)));
        return new RemotePage(language, info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig());
    }

    /**
     * Get a set of pages by their ids
     * @param language the language of the pages
     * @param pageIds a Collection of page ids
     * @return a map of ids to pages
     * @throws org.wikapidia.core.dao.DaoException if there was an error retrieving the pages
     */
    public Map<Integer, RemotePage> getByIds(Language language, Collection<Integer> pageIds) throws DaoException{
        Map<Integer, RemotePage> pageMap = new HashMap<Integer, RemotePage>();
        for(Integer pageId : pageIds){
            QueryType info = getPageInfo(getInfoByQuery(getQueryByID(pageId, language)));
            pageMap.put(pageId, new RemotePage(language, info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig()));
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
    public Map<Title, RemotePage> getByTitles(Language language, Collection<Title> titles, NameSpace ns) throws DaoException{
        Map<Title, RemotePage> pageMap = new HashMap<Title, RemotePage>();
        for(Title title : titles){
            QueryType info = getPageInfo(getInfoByQuery(getQueryByTitle(title, language)));
            pageMap.put(title, new RemotePage(language, info.getId(), info.getTitle(), info.getNameSpace(), info.isRedirect(), info.isDisambig()));
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
        QueryType info = getPageInfo(getInfoByQuery(getQueryByTitle(new Title(title, language), language)));
        return info.getId();
    }


    /**
     * Get an id from a title. Returns -1 if it doesn't exist.
     * @param title
     * @return
     */
    public int getIdByTitle(Title title) throws DaoException{
        QueryType info = getPageInfo(getInfoByQuery(getQueryByTitle(title)));
        return info.getId();
    }



    private class QueryType{
        QueryType(Long pageid, String title, String pagelanguage, Long ns, boolean isRedirect, boolean isDisambig){
            this.pageid = pageid;
            this.title = title;
            this.pagelanguage = pagelanguage;
            this.ns = ns;
            this.isRedirect = isRedirect;
            this.isDisambig = isDisambig;

        }
        QueryType(){};

        public int getId(){
            return pageid.intValue();
        }
        public Title getTitle(){
            return new Title(title, Language.getByLangCode(pagelanguage));
        }
        public NameSpace getNameSpace(){
            return NameSpace.getNameSpaceByArbitraryId(ns.intValue());
        }
        public boolean isRedirect(){
            return isRedirect;
        }

        public boolean isDisambig(){
            return isDisambig;
        }

        private Long pageid;
        private String title;
        private String pagelanguage;
        private Long ns;
        private boolean isRedirect;
        private boolean isDisambig;

    }


    private QueryType getPageInfo(String text){
        Gson gson = new Gson();
        JsonParser jp = new JsonParser();
        JsonObject test = jp.parse(text).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> pageSet = jp.parse(text).getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().entrySet();
        QueryType query = new QueryType();
        for (Map.Entry<String, JsonElement> entry: pageSet)
        {
            Long pageid = entry.getValue().getAsJsonObject().get("pageid").getAsLong();
            String title = entry.getValue().getAsJsonObject().get("title").getAsString();
            String pagelanguage = entry.getValue().getAsJsonObject().get("pagelanguage").getAsString();
            Long ns = entry.getValue().getAsJsonObject().get("ns").getAsLong();
            Boolean isRedirect = entry.getValue().getAsJsonObject().has("redirect");
            Boolean isDisambig = entry.getValue().getAsJsonObject().get("title").getAsString().contains("(disambiguation)");
            query = new QueryType(pageid, title, pagelanguage, ns, isRedirect, isDisambig);
        }

        return query;
    }

    private String getQueryByTitle(Title title, Language language){
        String http = new String("http://");
        String host = new String(".wikipedia.org");
        String query = new String("/w/api.php?action=query&prop=info&format=json&titles=");
        if(followRedirects)
            return http + language.getLangCode() + host + query + title.getCanonicalTitle().replaceAll(" ", "_") + "&redirects=";
        else
            return http + language.getLangCode() + host + query + title.getCanonicalTitle().replaceAll(" ", "_");

    }

    private String getQueryByTitle(Title title){
        String http = new String("http://");
        String host = new String(".wikipedia.org");
        String query = new String("/w/api.php?action=query&prop=info&format=json&titles=");
        //default language : en
        if(followRedirects)
            return http + "en" + host + query + title.getCanonicalTitle().replaceAll(" ", "_");
        else
            return http + "en" + host + query + title.getCanonicalTitle().replaceAll(" ", "_")+ "&redirects=";
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

    private String getInfoByQuery(String query){
        String info = new String();
        try{
            info = GetTextByURL.getText(query);
        }
        catch(Exception e){
            System.out.println("Error get info from wiki server");
        }
        return info;
    }


}
