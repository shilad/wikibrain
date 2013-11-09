package org.wikapidia.core.dao.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/2/13
 * Time: 12:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class LiveUtils {

    /**
     * queries the wikipedia server for JSON output that can be parsed to create a wikAPIdia data object
     * @param language
     * @param queryArgs specifies what should be queried for (links, pages, etc.)
     * @return
     * @throws DaoException
     */
    public static String getQueryJson(Language language, String queryArgs) throws DaoException {
        String http = "http://";
        String host = ".wikipedia.org";
        String query = http + language.getLangCode() + host + "/w/api.php?action=query&" + "&format=json&" + queryArgs;
        return getInfoByQuery(query);
    }

    public static String getInfoByQuery(String query) throws DaoException {
        String info = new String();
        InputStream inputStr;
        try{
            inputStr = new URL(query).openStream();
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

        return info;
    }

    /**
     *
     * @param text
     * @return
     */
    public static JsonObject parseQueryObject(String text) {
        JsonParser jp = new JsonParser();
        return jp.parse(text).getAsJsonObject().get("query").getAsJsonObject();
    }

    public static JsonObject getJsonObjectFromQueryObject(JsonObject queryObject, String newObjectRoot) throws DaoException {
        if (queryObject.get(newObjectRoot).isJsonObject()) {
            return queryObject.get(newObjectRoot).getAsJsonObject();
        }
        else {
            throw new DaoException("Requested JSON can't be parsed into object");
        }
    }

    public static JsonArray getJsonArrayFromQueryObject(JsonObject queryObject, String arrayRoot) throws DaoException {
        if (queryObject.get(arrayRoot).isJsonArray()) {
            return queryObject.get(arrayRoot).getAsJsonArray();
        }
        else {
            throw new DaoException("Requested JSON can't be parsed into array");
        }
    }

    public static List<String> getValuesFromJsonObject(JsonObject jo, String valueType) {
        List<String> values = new ArrayList<String>();
        Set<Map.Entry<String, JsonElement>> valueSet = jo.entrySet();

        for (Map.Entry<String, JsonElement> entry: valueSet) {
            String value = entry.getValue().getAsJsonObject().get(valueType).getAsString();
            values.add(value);
        }
        return values;
    }

    public static List<String> getValuesFromJsonArray(JsonArray ja, String valueType) {
        List<String> values = new ArrayList<String>();

        for (JsonElement elem : ja) {
            String value = elem.getAsJsonObject().get(valueType).getAsString();
            values.add(value);
        }

        return values;
    }
}
