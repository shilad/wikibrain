package org.wikapidia.core.dao.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.wikapidia.core.dao.DaoException;

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
        JsonObject continueObject1 = continueElem.getAsJsonObject();
        JsonObject continueObject2 = continueObject1.get(queryType).getAsJsonObject();
        return continueObject2.get(continueParam).getAsString();
    }

    public void getQueryReturnValuesAsStrings(String queryResult, String queryResultDataSection, String valueType, List<String> values) throws DaoException {
        //get JSON object representing query reply
        JsonObject queryReplyObject = parseQueryObject(queryResult, "query");
        //parse desired values from JSON object into string list
        JsonElement dataSectionElem = queryReplyObject.get(queryResultDataSection);
        if (dataSectionElem.isJsonArray()) {
            JsonArray array = getJsonArrayFromQueryObject(queryReplyObject, queryResultDataSection);
            getStringsFromJsonArray(array, valueType, values);
        }
        else {
            JsonObject object = getJsonObjectFromQueryObject(queryReplyObject, queryResultDataSection);
            getStringsFromJsonObject(object, valueType, values);
        }
    }

    public void getQueryReturnValuesAsInts(String queryResult, String queryResultDataSection, String valueType, List<Integer> values) throws DaoException {
        //get JSON object representing query reply
        JsonObject queryReplyObject = parseQueryObject(queryResult, "query");
        //parse desired values from JSON object into string list
        JsonElement dataSectionElem = queryReplyObject.get(queryResultDataSection);
        if (dataSectionElem.isJsonArray()) {
            JsonArray array = getJsonArrayFromQueryObject(queryReplyObject, queryResultDataSection);
            getIntsFromJsonArray(array, valueType, values);
        }
        else {
            JsonObject object = getJsonObjectFromQueryObject(queryReplyObject, queryResultDataSection);
            getIntsFromJsonObject(object, valueType, values);
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
    private void getStringsFromJsonObject(JsonObject jo, String valueType, List<String> values) {
        Set<Map.Entry<String, JsonElement>> valueSet = jo.entrySet();
        for (Map.Entry<String, JsonElement> entry: valueSet) {
            String value = entry.getValue().getAsJsonObject().get(valueType).getAsString();
            values.add(value);
        }
    }

    private void getStringsFromJsonArray(JsonArray ja, String valueType, List<String> values) {
        for (JsonElement elem : ja) {
            String value = elem.getAsJsonObject().get(valueType).getAsString();
            values.add(value);
        }
    }

    private void getIntsFromJsonObject(JsonObject jo, String valueType, List<Integer> values) {
        Set<Map.Entry<String, JsonElement>> valueSet = jo.entrySet();
        for (Map.Entry<String, JsonElement> entry: valueSet) {
            Integer value = entry.getValue().getAsJsonObject().get(valueType).getAsInt();
            values.add(value);
        }
    }

    private void getIntsFromJsonArray(JsonArray ja, String valueType, List<Integer> values) {
        for (JsonElement elem : ja) {
            Integer value = elem.getAsJsonObject().get(valueType).getAsInt();
            values.add(value);
        }
    }
}
