package org.wikapidia.core.dao.live;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/11/13
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final QueryParser parser;
    private final boolean parseArray; //if true, the queryResultDataSection is parsed as a JSON array; parsed as a JSON object if false
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
                this.parser = new ObjectQueryParser();
                break;
            case CATEGORYMEMBERS:
                this.queryAction = "list";
                this.queryType = "categorymembers";
                this.queryPagePrefix = "cm";
                this.queryPrefix = "cm";
                this.pluralPage = false;
                this.queryResultDataSection = "categorymembers";
                this.parser = new ArrayQueryParser();
                break;
            case CATEGORIES:
                this.queryAction = "prop";
                this.queryType = "categories";
                this.queryPagePrefix = "";
                this.queryPrefix = "cl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                this.parser = new ObjectQueryParser();
                break;
            case LINKS:
                this.queryAction = "generator";
                this.queryType = "links";
                this.queryPagePrefix = "";
                this.queryPrefix = "gpl";
                this.pluralPage = true;
                this.queryResultDataSection = "pages";
                this.parser = new ObjectQueryParser();
                break;
            case BACKLINKS:
                this.queryAction = "list";
                this.queryType = "backlinks";
                this.queryPagePrefix = "bl";
                this.queryPrefix = "bl";
                this.pluralPage = false;
                this.queryResultDataSection = "backlinks";
                this.parser = new ArrayQueryParser();
                break;
            default:    //allpages
                this.queryAction = "list";
                this.queryType = "allpages";
                this.queryPagePrefix = "ap";
                this.queryPrefix = "ap";
                this.pluralPage = false;
                this.queryResultDataSection = "allpages";
                this.parser = new ArrayQueryParser();
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
        if (queryResult.equals("")) {
            getRawQueryText();
        }
        //get JSON object representing query reply
        JsonObject queryReplyObject = parseQueryObject(queryResult);
        
        //parse desired values from JSON object into string list
        if (parseArray) {
            return getStringsFromJsonArray(getJsonArrayFromQueryObject(queryReplyObject, queryResultDataSection), valueType);
        }
        else {
            return getStringsFromJsonObject(getJsonObjectFromQueryObject(queryReplyObject, queryResultDataSection), valueType);
        }
    }

    /**
     * same as above method, but returns an int list instead of a string list
     * @param valueType specifies which values from the query result to return (pageids, titles, etc.)
     * @return string list containing the values of interest, which are specified by valueType
     * @throws DaoException
     */
    public List<Integer> getIntsFromQueryResult(String valueType) throws DaoException {
        if (queryResult.equals("")) {
            getRawQueryText();
        }
        //get JSON object representing query reply
        JsonObject queryReplyObject = parseQueryObject(queryResult);
        
        //parse desired values from JSON object into int list
        if (parseArray) {
            return getIntsFromJsonArray(getJsonArrayFromQueryObject(queryReplyObject, queryResultDataSection), valueType);
        }
        else {
            return getIntsFromJsonObject(getJsonObjectFromQueryObject(queryReplyObject, queryResultDataSection), valueType);
        }
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

     /**
     *
     * @param text raw query output JSON
     * @return JSON object representing the query result
     */
    private JsonObject parseQueryObject(String text) {
        JsonParser jp = new JsonParser();
        return jp.parse(text).getAsJsonObject().get("query").getAsJsonObject();
    }

    /**
     * utility method used to get a JSON object containing desired query information
     * @param queryObject
     * @param newObjectKey text representing the key of the new object to return
     * @return a new JSON object within the input queryObject
     * @throws DaoException
     */
    private JsonObject getJsonObjectFromQueryObject(JsonObject queryObject, String newObjectKey) throws DaoException {
        if (queryObject.get(newObjectKey).isJsonObject()) {
            return queryObject.get(newObjectKey).getAsJsonObject();
        }
        else {
            throw new DaoException("Requested JSON can't be parsed into object");
        }
    }

    /**
     * utility method used to get a JSON array containing desired query information
     * @param queryObject
     * @param arrayRoot text representing the name of the array
     * @return a JSON array within the input queryObject
     * @throws DaoException
     */
    private JsonArray getJsonArrayFromQueryObject(JsonObject queryObject, String arrayRoot) throws DaoException {
        if (queryObject.get(arrayRoot).isJsonArray()) {
            return queryObject.get(arrayRoot).getAsJsonArray();
        }
        else {
            throw new DaoException("Requested JSON can't be parsed into array");
        }
    }

    /**
     * The following four methods take either a JSON object (jo) or JSON array (ja) containing the desired information
     * of the query result. This information is returned as a list of string or int values. The paramater "valueType"
     * specifies which things in the query result should be returned in the list
     */

    private List<String> getStringsFromJsonObject(JsonObject jo, String valueType) {
        List<String> values = new ArrayList<String>();
        Set<Map.Entry<String, JsonElement>> valueSet = jo.entrySet();

        for (Map.Entry<String, JsonElement> entry: valueSet) {
            String value = entry.getValue().getAsJsonObject().get(valueType).getAsString();
            values.add(value);
        }
        return values;
    }

    private List<String> getStringsFromJsonArray(JsonArray ja, String valueType) {
        List<String> values = new ArrayList<String>();

        for (JsonElement elem : ja) {
            String value = elem.getAsJsonObject().get(valueType).getAsString();
            values.add(value);
        }

        return values;
    }

    private List<Integer> getIntsFromJsonObject(JsonObject jo, String valueType) {
        List<Integer> values = new ArrayList<Integer>();
        Set<Map.Entry<String, JsonElement>> valueSet = jo.entrySet();

        for (Map.Entry<String, JsonElement> entry: valueSet) {
            Integer value = entry.getValue().getAsJsonObject().get(valueType).getAsInt();
            values.add(value);
        }
        return values;
    }

    private List<Integer> getIntsFromJsonArray(JsonArray ja, String valueType) {
        List<Integer> values = new ArrayList<Integer>();

        for (JsonElement elem : ja) {
            Integer value = elem.getAsJsonObject().get(valueType).getAsInt();
            values.add(value);
        }

        return values;
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

