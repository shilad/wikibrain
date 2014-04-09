package org.wikapidia.wikidata;

import com.google.gson.*;
import org.apache.commons.lang3.time.DateUtils;
import org.wikapidia.parser.WpParseException;

import java.text.ParseException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class JsonUtils {
    public static final WikidataValue jsonToValue(String type, JsonElement element) throws WpParseException {
        type = type.toLowerCase();
        if (type.equals("string")) {
            return new WikidataValue(WikidataValue.Type.STRING, element.getAsString(), element);
        } else if (type.equals("somevalue")) {
            return new WikidataValue(WikidataValue.Type.SOMEVALUE, null, JsonNull.INSTANCE);
        } else if (type.equals("novalue")) {
            return new WikidataValue(WikidataValue.Type.NOVALUE, null, JsonNull.INSTANCE);
        } else if (type.equals("int")) {
            return new WikidataValue(WikidataValue.Type.INT, element.getAsJsonPrimitive().getAsInt(), element);
        } else if (type.equals("time")) {
            // format is "+00000001997-11-27T00:00:00Z"
            String time = element.getAsJsonObject().get("time").getAsString();
            if (time.startsWith("+")) {
                time = time.substring(1) + "AD";
            } else if (time.startsWith("-")) {
                time = time.substring(1) + "BC";
            } else {
                throw new WpParseException("Invalid date: " + element);
            }
            try {
                return new WikidataValue(
                        WikidataValue.Type.TIME,
                        DateUtils.parseDate(time, "yyyyyyyyyyy-MM-dd'T'HH:mm:ss'Z'G"),
                        element
                );
            } catch (ParseException e) {
                throw new WpParseException("Invalid date: " + element);
            }
        } else if (type.equals("wikibase-entityid") || type.equals("item")) {
            String entityType = element.getAsJsonObject().get("entity-type").getAsString();
            if (!entityType.equals("item")) {
                throw new WpParseException("unknown entity type: " + entityType + " in " + element);
            }
            return new WikidataValue(
                    WikidataValue.Type.ITEM,
                    element.getAsJsonObject().get("numeric-id").getAsInt(),
                    element
            );
        } else if (Arrays.asList("globecoordinate", "other", "quantity").contains(type)) {
            return new WikidataValue(type, gsonToPrimitive(element), element);
        } else {
            throw new WpParseException("unknown wikidata type: " + type);
        }
    }

    public static Object gsonToPrimitive(JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isString()) {
                return prim.getAsString();
            } else if (prim.isBoolean()) {
                return prim.getAsBoolean();
            } else if (prim.isNumber()) {
                return prim.getAsInt();
            } else {
                throw new IllegalArgumentException("Unknown Gson primitive: " + prim);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < array.size(); i++) {
                list.add(gsonToPrimitive(array.get(i)));
            }
            return list;
        } else if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new HashMap<String, Object>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), gsonToPrimitive(entry.getValue()));
            }
            return map;
        } else {
            throw new IllegalArgumentException("Unknown Gson value: " + element);
        }
    }
}
