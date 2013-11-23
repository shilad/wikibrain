package org.wikapidia.core.dao.live;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/11/13
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */

import org.apache.commons.io.IOUtils;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * utility class used by LiveAPI DAOs to query the wikipedia server and retrieve results in a useful format
 */
public class LiveAPIQuery {

    private final Language lang;
    private final String outputFormat = "json"; //only JSON currently supported
    private final String queryAction;
    private final String queryType;
    private final String queryPagePrefix;
    private final String queryPrefix;
    private final Boolean pluralPage;
    private final String queryResultDataSection; //section of the query result containing the data of interest
    private QueryParser parser = new QueryParser();
    private Boolean redirects;
    private String title;
    private Integer pageid;
    private String filterredir;
    private String from;

    private String queryUrl;
    private String queryResult = ""; //text representing the raw output of the query

    private LiveAPIQuery(LiveAPIQueryBuilder builder) {
        this.lang = builder.lang;
        if (builder.redirects != null) {
            this.redirects = builder.redirects;
        }
        if (builder.title != null) {
            this.title = builder.title;
        }
        if (builder.pageid != null) {
            this.pageid = builder.pageid;
        }
        if (builder.filterredir != null) {
            this.filterredir = builder.filterredir;
        }
        if (builder.from != null) {
            this.from = builder.from;
        }

        switch (builder.queryType) {
            case 0: //INFO:
                this.queryAction = "prop";
                this.queryType = "info";
                this.queryPagePrefix = "";
                this.queryPrefix = "in";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                //this.parser = new InfoQueryParser();
                break;
            case 1: //CATEGORYMEMBERS:
                this.queryAction = "list";
                this.queryType = "categorymembers";
                this.queryPagePrefix = "cm";
                this.queryPrefix = "cm";
                this.pluralPage = false;
                this.queryResultDataSection = "categorymembers";
                //this.parser = new CategoryMemberQueryParser();
                break;
            case 2: //CATEGORIES:
                this.queryAction = "generator";
                this.queryType = "categories";
                this.queryPagePrefix = "";
                this.queryPrefix = "gcl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                //this.parser = new CategoryQueryParser();
                break;
            case 3: //LINKS:
                this.queryAction = "generator";
                this.queryType = "links";
                this.queryPagePrefix = "";
                this.queryPrefix = "gpl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                //this.parser = new LinkQueryParser();
                break;
            case 4: //BACKLINKS:
                this.queryAction = "list";
                this.queryType = "backlinks";
                this.queryPagePrefix = "bl";
                this.queryPrefix = "bl";
                this.pluralPage = false;
                this.queryResultDataSection = "backlinks";
                //this.parser = new BacklinkQueryParser();
                break;
            default:    //allpages
                this.queryAction = "list";
                this.queryType = "allpages";
                this.queryPagePrefix = "ap";
                this.queryPrefix = "ap";
                this.pluralPage = false;
                this.queryResultDataSection = "allpages";
                //this.parser = new AllpagesQueryParser();
                break;
        }
        constructQueryUrl();
    }

    public void constructQueryUrl() {
        String http = "http://";
        String host = ".wikipedia.org";
        String queryUrl = http + lang.getLangCode() + host + "/w/api.php?action=query&format=" + outputFormat +
                "&" + queryAction + "=" + queryType + "&" + queryPrefix + "limit=500";
        if (this.title != null) {
            queryUrl += "&" + queryPagePrefix + "title" + (pluralPage ? "s" : "") + "=" + title;
        }
        if (this.pageid != null) {
            queryUrl += "&" + queryPagePrefix + "pageid" + (pluralPage ? "s" : "") + "=" + pageid;
        }
        if ((this.redirects != null) && this.redirects) {
            queryUrl += "&redirects=";
        }
        if (this.filterredir != null) {
            queryUrl += "&" + queryPagePrefix + "filterredir" + (pluralPage ? "s" : "") + "=" + filterredir;
        }
        if (this.from != null) {
            queryUrl += "&" + queryPagePrefix + "from" + (pluralPage ? "s" : "") + "=" + from;
        }
        this.queryUrl = queryUrl;
    }

    /**
     * method used by client DAOs to retrieve a list of strings representing the values of interest returned by the query
     * @return string list containing the values of interest, which are specified by valueType
     * @throws DaoException
     */    
    public List<QueryReply> getValuesFromQueryResult() throws DaoException {
        List<QueryReply> values = new ArrayList<QueryReply>();
        String queryContinue = "";
        boolean hasContinue;
        do {
            getRawQueryText(queryUrl + queryContinue);
            parser.getQueryReturnValues(lang, queryResult, queryResultDataSection, values);
            queryContinue = parser.getContinue(queryResult, queryType, queryPrefix);
            hasContinue = (!queryContinue.equals(""));
            queryContinue = "&" + queryPrefix + "continue=" + queryContinue;
        }
        while (hasContinue);
        return values;
    }

    /**
     * queries the wikipedia server for text output that can be parsed to create a wikAPIdia data object
     * sets the class attribute queryResult to the value of this raw output
     * @return
     * @throws org.wikapidia.core.dao.DaoException
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
                throw new DaoException("Error parsing URL");
            }
            finally {
                IOUtils.closeQuietly(inputStr);
            }
        }
        catch(Exception e){
            throw new DaoException("Error getting page from the Wikipedia Server ");
        }

        queryResult = info;
    }

    //This class uses the builder method in anticipation of increased complexity over time
    public static class LiveAPIQueryBuilder {
        private final Language lang;
        //private final QueryType queryType;
        private final Integer queryType;
        private Boolean redirects;
        private String title;
        private Integer pageid;
        private String filterredir;
        private String from;
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
        }

        public LiveAPIQueryBuilder setRedirects(Boolean redirects) {
            this.redirects = redirects;
            return this;
        }

        public LiveAPIQueryBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public LiveAPIQueryBuilder setPageid(Integer pageid) {
            this.pageid = pageid;
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

        public LiveAPIQuery build() {
            return new LiveAPIQuery(this);
        }
    }
}

