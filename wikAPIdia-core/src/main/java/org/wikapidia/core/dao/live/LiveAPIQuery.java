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
import java.util.List;

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
            case INFO:
                this.queryAction = "prop";
                this.queryType = "info";
                this.queryPagePrefix = "";
                this.queryPrefix = "in";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                //this.parser = new ObjectQueryParser();
                break;
            case CATEGORYMEMBERS:
                this.queryAction = "list";
                this.queryType = "categorymembers";
                this.queryPagePrefix = "cm";
                this.queryPrefix = "cm";
                this.pluralPage = false;
                this.queryResultDataSection = "categorymembers";
                //this.parser = new ArrayQueryParser();
                break;
            case CATEGORIES:
                this.queryAction = "prop";
                this.queryType = "categories";
                this.queryPagePrefix = "";
                this.queryPrefix = "cl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                //this.parser = new ObjectQueryParser();
                break;
            case LINKS:
                this.queryAction = "generator";
                this.queryType = "links";
                this.queryPagePrefix = "";
                this.queryPrefix = "gpl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                //this.parser = new ObjectQueryParser();
                break;
            case BACKLINKS:
                this.queryAction = "list";
                this.queryType = "backlinks";
                this.queryPagePrefix = "bl";
                this.queryPrefix = "bl";
                this.pluralPage = false;
                this.queryResultDataSection = "backlinks";
                //this.parser = new ArrayQueryParser();
                break;
            default:    //allpages
                this.queryAction = "list";
                this.queryType = "allpages";
                this.queryPagePrefix = "ap";
                this.queryPrefix = "ap";
                this.pluralPage = false;
                this.queryResultDataSection = "allpages";
                //this.parser = new ArrayQueryParser();
                break;
        }
        constructQueryUrl();
    }

    public void constructQueryUrl() {
        String http = "http://";
        String host = ".wikipedia.org";
        String queryUrl = http + lang.getLangCode() + host + "/w/api.php?action=query&format=" + outputFormat +
                "&" + queryAction + "=" + queryType + "&" + queryPagePrefix + "limit=500";
        if (this.title != null) {
            queryUrl += "&" + queryPagePrefix + "title" + (pluralPage ? "s" : "") + "=" + title;
        }
        if (this.pageid != null) {
            queryUrl += "&" + queryPagePrefix + "pageid" + (pluralPage ? "s" : "") + "=" + pageid;
        }
        if (this.redirects != null) {
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
     * @param valueType specifies which values from the query result to return (pageids, titles, etc.)
     * @return string list containing the values of interest, which are specified by valueType
     * @throws DaoException
     */    
    public List<String> getStringsFromQueryResult(String valueType) throws DaoException {
        List<String> values = new ArrayList<String>();
        String queryContinue;
        boolean hasContinue;
        do {
            getRawQueryText();
            parser.getQueryReturnValuesAsStrings(queryResult, queryResultDataSection, valueType, values);
            queryContinue = parser.getContinue(queryResult, queryType, queryPrefix);
            hasContinue = (queryContinue.equals(""));
            if (hasContinue) {
                queryUrl += "&" + queryPrefix + "continue=" + queryContinue;
            }
        }
        while (hasContinue);
        return values;
    }

    /**
     * same as above method, but returns an int list instead of a string list
     * @param valueType specifies which values from the query result to return (pageids, titles, etc.)
     * @return string list containing the values of interest, which are specified by valueType
     * @throws DaoException
     */
    public List<Integer> getIntsFromQueryResult(String valueType) throws DaoException {
        List<Integer> values = new ArrayList<Integer>();
        String queryContinue;
        boolean hasContinue;
        do {
            getRawQueryText();
            parser.getQueryReturnValuesAsInts(queryResult, queryResultDataSection, valueType, values);
            queryContinue = parser.getContinue(queryResult, queryType, queryPrefix);
            hasContinue = (queryContinue.equals(""));
            if (hasContinue) {
                queryUrl += "&" + queryPrefix + "continue=" + queryContinue;
            }
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
    private void getRawQueryText() throws DaoException {
        String info = new String();
        InputStream inputStr;

        try{
            inputStr = new URL(queryUrl).openStream();
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
        private final QueryType queryType;
        private Boolean redirects;
        private String title;
        private Integer pageid;
        private String filterredir;
        private String from;

        public LiveAPIQueryBuilder(QueryType queryType, Language lang) {
            this.queryType = queryType;
            this.lang = lang;
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

