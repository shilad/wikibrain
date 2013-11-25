package org.wikapidia.core.dao.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * utility class used by LiveAPIQuery to parse QueryReply objects from raw query output JSON
 * author: derian
 */

public class QueryParser {

    JsonParser jp = new JsonParser();

    /**
     * Retrieves the string necessary to append to a query URL to retrieve pages starting after the last page
     * retrieved in the previous query. If the last page alphabetically was retrieved in the previous query, there will be 
     * no continue string.
     * @param queryResult represents the raw text output of the previous query
     * @param queryType
     * @param prefix
     * @return the query continue string, or "" if no continue string is found
     */
    public String getContinue(String queryResult, String queryType, String prefix) {
        String continueParam = prefix + "continue";
        JsonElement continueElem = jp.parse(queryResult).getAsJsonObject().get("query-continue");
        if (continueElem == null) {
            return "";
        }
        return continueElem.getAsJsonObject().get(queryType).getAsJsonObject().get(continueParam).getAsString();
    }

    /**
     * adds QueryReply objects parsed from raw query result text to an input QueryReply list
     * @param lang
     * @param queryResult raw text output of query
     * @param queryResultDataSection //section of queryResult in which values of interest can be found
     * @param values list to which parsed QueryReply objects should be added
     * @throws DaoException
     */
    public void getQueryReturnValues(Language lang, String queryResult, String queryResultDataSection, List<QueryReply> values) throws DaoException {
        //get JSON object representing query reply
        JsonObject queryReplyObject = parseQueryObject(queryResult, "query");
        //parse desired values from JSON object into QueryReplies and add to values
        JsonElement dataSectionElem = queryReplyObject.get(queryResultDataSection);
        if (dataSectionElem.isJsonArray()) {
            JsonArray array = getJsonArrayFromQueryObject(queryReplyObject, queryResultDataSection);
            getValuesFromJsonArray(array, values);
        }
        else {
            JsonObject object = getJsonObjectFromQueryObject(queryReplyObject, queryResultDataSection);
            getValuesFromJsonObject(object, values);
        }
    }

    /**
     *
     * @param text raw query output JSON
     * @return JSON object representing the query result
     */
    public JsonObject parseQueryObject(String text, String value) {
        return jp.parse(text).getAsJsonObject().get(value).getAsJsonObject();
    }

    public JsonArray parseJsonArray(String text) {
        return jp.parse(text).getAsJsonArray();
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
     * The following two methods take either a JSON object or JSON array containing desired information
     * from a query result. Page ID, title, namespace, redirect, and disambiguation information are retrieved from each
     * page in the result, and used to create a QueryReply which is added to values
     */
    private void getValuesFromJsonObject(JsonObject jo, List<QueryReply> values) {
        Set<Map.Entry<String, JsonElement>> valueSet = jo.entrySet();
        for (Map.Entry<String, JsonElement> entry: valueSet) {
           QueryReply reply = getQueryReplyFromJsonElement(entry.getValue());
           values.add(reply);
        }
    }

    private void getValuesFromJsonArray(JsonArray ja, List<QueryReply> values) {
        for (JsonElement elem : ja) {
            QueryReply reply = getQueryReplyFromJsonElement(elem);
            values.add(reply);
        }
    }

    /**
     * parses a QueryReply object from a JsonElement representing a page from a query result
     * @param queryReplyPage JsonElement representing page of query result
     * @return QueryReply containing useful information about this page
     */
    private QueryReply getQueryReplyFromJsonElement(JsonElement queryReplyPage) {
        JsonObject entryValue = queryReplyPage.getAsJsonObject();
        JsonElement entryPageid = entryValue.get("pageid");
        JsonElement entryTitle = entryValue.get("title");
        JsonElement entryNamespace = entryValue.get("ns");

        boolean isRedirect = entryValue.has("redirect");
        int pageid = (entryPageid != null ? entryPageid.getAsInt() : -1);
        String title = (entryTitle != null ? entryTitle.getAsString() : "");
        boolean isDisambig = title.contains("(disambiguation)");
        int namespace = (entryNamespace != null ? entryNamespace.getAsInt() : -1);

        return new QueryReply(pageid, title, namespace, isRedirect, isDisambig);
    }
 }
