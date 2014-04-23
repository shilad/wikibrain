package org.wikibrain.core.dao.live;

/**
 * utility class used by LiveAPI DAOs to query the wikipedia server and retrieve results as a list of QueryReply objects
 * author: derian
 */

import org.apache.commons.io.IOUtils;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiveAPIQuery {

    private final Language lang;
    private final String outputFormat = "json"; //only JSON currently supported    
    private final String queryAction;
    private final String queryType;
    private final String queryInfoPrefix; //prefix before params in URL string specifying what info should be returned
    private final String queryLimitPrefix; //prefix before "limit" and "continue" params in URL string
                                           //usually the same as queryInfoPrefix, but not in the case of prop and generator queries
    private final Boolean pluralPage; //if true, query URL string must contain plural version of pageid, title, etc as a param
    private final String queryResultDataSection; //section of the query result containing the data of interest
    private QueryParser parser = new QueryParser();
    private Boolean redirects;
    private List<String> titles;
    private List<Integer> pageids;
    private String filterredir;
    private String from;
    private Integer namespace;
    private String prop = null; //only used in all-links queries, ensures that ids and titles of links will be returned

    private String queryUrl;
    private String queryResult = ""; //text representing the raw output of the query

    private LiveAPIQuery(LiveAPIQueryBuilder builder) {
        this.lang = builder.lang;
        if (builder.redirects != null) {
            this.redirects = builder.redirects;
        }
        if (builder.titles != null) {
            this.titles = builder.titles;
        }
        if (builder.pageids != null) {
            this.pageids = builder.pageids;
        }
        if (builder.filterredir != null) {
            this.filterredir = builder.filterredir;
        }
        if (builder.from != null) {
            this.from = builder.from;
        }
        if (builder.namespace != null) {
            this.namespace = builder.namespace;
        }

        // set parameters for the URL string according to the query type
        switch (builder.queryType) {
            case 0: //INFO:
                this.queryAction = "prop";
                this.queryType = "info";
                this.queryInfoPrefix = "";
                this.queryLimitPrefix = "in";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                break;
            case 1: //CATEGORYMEMBERS:
                this.queryAction = "list";
                this.queryType = "categorymembers";
                this.queryInfoPrefix = "cm";
                this.queryLimitPrefix = "cm";
                this.pluralPage = false;
                this.queryResultDataSection = "categorymembers";
                break;
            case 2: //CATEGORIES:
                this.queryAction = "generator";
                this.queryType = "categories";
                this.queryInfoPrefix = "";
                this.queryLimitPrefix = "gcl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                break;
            case 3: //LINKS:
                this.queryAction = "generator";
                this.queryType = "links";
                this.queryInfoPrefix = "";
                this.queryLimitPrefix = "gpl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                break;
            case 4: //BACKLINKS:
                this.queryAction = "list";
                this.queryType = "backlinks";
                this.queryInfoPrefix = "bl";
                this.queryLimitPrefix = "bl";
                this.pluralPage = false;
                this.queryResultDataSection = "backlinks";
                break;
            case 5:    //ALLPAGES
                this.queryAction = "list";
                this.queryType = "allpages";
                this.queryInfoPrefix = "ap";
                this.queryLimitPrefix = "ap";
                this.pluralPage = false;
                this.queryResultDataSection = "allpages";
                break;
            default:    //ALLLINKS
                this.queryAction = "list";
                this.queryType = "alllinks";
                this.queryInfoPrefix = "al";
                this.queryLimitPrefix = "al";
                this.pluralPage = false;
                this.queryResultDataSection = "alllinks";
                this.prop = "ids|title";
                break;
        }
        constructQueryUrl();
    }

    public void constructQueryUrl() {
        String http = "http://";
        String host = ".wikipedia.org";
        String queryUrl = http + lang.getLangCode() + host + "/w/api.php?action=query&format=" + outputFormat +
                "&" + queryAction + "=" + queryType + "&" + queryLimitPrefix + "limit=500";
        if (!this.titles.isEmpty()) {
            queryUrl += "&" + queryInfoPrefix + "title" + (pluralPage ? "s" : "") + "=" + titles.get(0);
            for (int i = 1; i < titles.size(); i++) {
                queryUrl += "|" + titles.get(i);
            }
        }
        if (!this.pageids.isEmpty()) {
            queryUrl += "&" + queryInfoPrefix + "pageid" + (pluralPage ? "s" : "") + "=" + pageids.get(0);
            for (int i = 1; i < pageids.size(); i++) {
                queryUrl += "|" + pageids.get(i);
            }
        }
        //if redirects is true, resolve redirects in the query result
        if ((this.redirects != null) && this.redirects) {
            queryUrl += "&redirects=";
        }
        //specify whether to return redirects, non-redirects, or both in the query result
        //default is both
        if (this.filterredir != null) {
            queryUrl += "&" + queryInfoPrefix + "filterredir" + "=" + filterredir;
        }
        if (this.from != null) {
            queryUrl += "&" + queryInfoPrefix + "from" + "=" + from;
        }
        if (this.namespace != null) {
            queryUrl += "&" + queryInfoPrefix + "namespace" + "=" + namespace;
        }
        if (this.prop != null) {
            queryUrl += "&" + queryInfoPrefix + "prop" + "=" + prop;
        }
        this.queryUrl = queryUrl;
    }

    /**
     * method used by client DAOs to retrieve a list of QueryReplies representing the values of interest returned by the query
     * @return QueryReply list containing the values of interest
     * @throws DaoException
     */    
    public List<QueryReply> getValuesFromQueryResult() throws DaoException {
        List<QueryReply> values = new ArrayList<QueryReply>();
        String queryContinue = "";
        boolean hasContinue;
        do {
            //make query and set this.queryResult to the resulting text
            getRawQueryText(queryUrl + queryContinue);

            //parse the queryResult and add the resulting QueryReply objects to values
            parser.getQueryReturnValues(lang, queryResult, queryResultDataSection, values);

            /*
             * Determine whether or not the query result contained continue info, meaning there were too many
             * values to return in one query
             * If so, continue parsing by adding the continue info to the URL string
             */
            queryContinue = parser.getContinue(queryResult, queryType, queryLimitPrefix);
            hasContinue = (!queryContinue.equals(""));
            queryContinue = "&" + queryLimitPrefix + "continue=" + queryContinue;
        }
        while (hasContinue);
        return values;
    }

    /**
     * queries the wikipedia server for text output that can be parsed to create a wikibrain data object
     * sets the class attribute queryResult to the value of this raw output
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    private void getRawQueryText(String url) throws DaoException {
        String info = new String();
        InputStream inputStr;

        try{
            inputStr = new URL(url).openStream();
            try {
                info = IOUtils.toString(inputStr);
            }
            catch(Exception e){
                throw new DaoException("Error parsing LiveDao query URL");
            }
            finally {
                IOUtils.closeQuietly(inputStr);
            }
        }
        catch(Exception e){
            throw new DaoException("Error getting page from the Wikipedia Server (Check your internet connection) ");
        }

        queryResult = info;
    }

    //Builder used by client DAOs to create instances of LiveAPIQuery
    public static class LiveAPIQueryBuilder {
        private final Language lang;
        //private final QueryType queryType;
        private final Integer queryType;
        private Boolean redirects;
        private List<String> titles = new ArrayList<String>();
        private List<Integer> pageids = new ArrayList<Integer>();
        private String filterredir;
        private String from;
        private Integer namespace;
        private Map<String, Integer> queryTypeMap = new HashMap<String, Integer>();

        public LiveAPIQueryBuilder(String queryType, Language lang) {
            initQueryTypeMap();
            this.queryType = queryTypeMap.get(queryType);
            this.lang = lang;
        }

        private void initQueryTypeMap() {
            queryTypeMap.put("INFO", 0);
            queryTypeMap.put("CATEGORYMEMBERS", 1);
            queryTypeMap.put("CATEGORIES", 2);
            queryTypeMap.put("LINKS", 3);
            queryTypeMap.put("BACKLINKS", 4);
            queryTypeMap.put("ALLPAGES", 5);
            queryTypeMap.put("ALLLINKS", 6);
        }

        public LiveAPIQueryBuilder setRedirects(Boolean redirects) {
            this.redirects = redirects;
            return this;
        }

        public LiveAPIQueryBuilder setTitles(List<String> titles) {
            this.titles = titles;
            return this;
        }

        public LiveAPIQueryBuilder setPageids(List<Integer> pageids) {
            this.pageids = pageids;
            return this;
        }

        public LiveAPIQueryBuilder addTitle(String title) {
            this.titles.add(title);
            return this;
        }

        public LiveAPIQueryBuilder addPageid(int pageid) {
            this.pageids.add(pageid);
            return this;
        }

        public LiveAPIQueryBuilder setFilterredir(String filterredir) {
            this.filterredir = filterredir;
            return this;
        }

        public LiveAPIQueryBuilder setFrom(String from) {
            this.from = from;
            return this;
        }

        public LiveAPIQueryBuilder setNamespace(int namespace) {
            this.namespace = namespace;
            return this;
        }

        public LiveAPIQuery build() {
            return new LiveAPIQuery(this);
        }
    }
}

