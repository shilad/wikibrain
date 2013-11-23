package org.wikapidia.core.dao.live;

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
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/18/13
 * Time: 2:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueryParser {

    JsonParser jp = new JsonParser();

    public String getContinue(String text, String queryType, String prefix) {
        String continueParam = prefix + "continue";
        JsonElement continueElem = jp.parse(text).getAsJsonObject().get("query-continue");
        if (continueElem == null) {
            return "";
        }
        return continueElem.getAsJsonObject().get(queryType).getAsJsonObject().get(continueParam).getAsString();
    }

    public void getQueryReturnValues(Language lang, String queryResult, String queryResultDataSection, List<QueryReply> values) throws DaoException {
        //get JSON object representing query reply
        JsonObject queryReplyObject = parseQueryObject(queryResult, "query");
        //parse desired values from JSON object into string list
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
     * The following four methods take either a JSON object (jo) or JSON array (ja) containing the desired information
     * of the query result. This information is returned as a list of string or int values. The paramater "valueType"
     * specifies which things in the query result should be returned in the list
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

    private QueryReply getQueryReplyFromJsonElement(JsonElement queryReplyElem) {
        List<Integer> categories = new ArrayList<Integer>();
        List<Integer> categorymembers = new ArrayList<Integer>();

        JsonObject entryValue = queryReplyElem.getAsJsonObject();
        JsonElement entryPageid = entryValue.get("pageid");
        JsonElement entryTitle = entryValue.get("title");
        JsonElement entryNamespace = entryValue.get("ns");
        JsonElement entryCategories = entryValue.get("categories");
        JsonElement entryCategorymembers = entryValue.get("categorymembers");
        JsonArray arrayCategories = (entryCategories != null ? entryCategories.getAsJsonArray() : new JsonArray());
        JsonArray arrayCategorymembers = (entryCategorymembers != null ? entryCategorymembers.getAsJsonArray() : new JsonArray());

        boolean isRedirect = entryValue.has("redirect");
        int pageid = (entryPageid != null ? entryPageid.getAsInt() : -1);
        String title = (entryTitle != null ? entryTitle.getAsString() : "");
        boolean isDisambig = title.contains("(disambiguation)");
        int namespace = (entryNamespace != null ? entryNamespace.getAsInt() : -1);

        for (JsonElement category : arrayCategories) {
            JsonElement categoryElem = category.getAsJsonObject().get("pageid");
            int categoryId = (categoryElem != null ? categoryElem.getAsInt() : -1);
            categories.add(categoryId);
        }

        for (JsonElement member : arrayCategorymembers) {
            JsonElement memberElem = member.getAsJsonObject().get("pageid");
            int memberId = (memberElem != null ? memberElem.getAsInt() : -1);
            categorymembers.add(memberId);
        }

        return new QueryReply(pageid, title, namespace, isRedirect, isDisambig, categories, categorymembers);
    }
 }
