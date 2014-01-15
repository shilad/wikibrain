package org.wikapidia.wikidata;

import com.google.gson.*;
import org.apache.commons.lang3.time.DateUtils;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.WpParseException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class WikidataParser {
    private static final Logger LOG = Logger.getLogger(WikidataParser.class.getName());
    private final LanguageSet langs;

    public WikidataParser() {
        this(LanguageSet.ALL);
    }

    public WikidataParser(LanguageSet langs) {
        this.langs = langs;
    }

    public WikidataEntity parse(RawPage rawPage) throws WpParseException {
        JsonParser parser = new JsonParser();

        JsonObject obj;
        try {
            obj = parser.parse(rawPage.getBody()).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            System.err.println("invalid page: " + rawPage.getBody());
            throw new WpParseException(e);
        }

        if (!obj.has("entity")) {
            throw new WpParseException("Page " + obj + " missing entity field");
        }
        WikidataEntity record = parseEntity(obj.get("entity"));

        for (Map.Entry<String, JsonElement> fields : obj.entrySet()) {
            String name = fields.getKey();
            if (name.equals("label")) {
                parseLabels(record, fields.getValue());
            } else if (name.equals("description")) {
                parseDescription(record, fields.getValue());
            } else if (name.equals("aliases")) {
                parseAliases(record, fields.getValue());
            } else if (name.equals("links")) {
                // do nothing for now
            } else if (name.equals("entity")) {
                // already handled
            } else if (name.equals("claims")) {
                parseClaims(record, fields.getValue());
            } else {
                LOG.log(Level.WARNING, "unexpected field '" + name + "' when parsing " + rawPage.getTitle());
            }
        }

        return record;
    }

    private void parseClaims(WikidataEntity record, JsonElement value) throws WpParseException {
        JsonArray array = value.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            try {
                WikidataStatement statement = parseOneClaim(record, array.get(i));
                record.getStatements().add(statement);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "parse error for " + array.get(i) + ":", e);
            }
        }
    }

    private WikidataStatement parseOneClaim(WikidataEntity item, JsonElement jsonClaim) throws WpParseException {
        JsonObject obj = jsonClaim.getAsJsonObject();

        if (!obj.has("m")) {
            throw new WpParseException("claim " + jsonClaim + " has no property m");
        }


        JsonArray jsonVal = obj.get("m").getAsJsonArray();

        WikidataEntity prop = new WikidataEntity(WikidataEntity.Type.PROPERTY, jsonVal.get(1).getAsInt());
        String valTypeStr = jsonVal.get(0).getAsString();
        WikidataValue value = null;
        if (valTypeStr.equals("value")) {
            value = parseValue(jsonVal.get(2).getAsString(), jsonVal.get(3));
        } else if (valTypeStr.equals("novalue")) {
            value = new WikidataValue(WikidataValue.Type.NOVALUE, null);
        } else if (valTypeStr.equals("somevalue")) {
            value = new WikidataValue(WikidataValue.Type.SOMEVALUE, null);
        } else {
            throw new WpParseException("claim " + jsonClaim + " has unknown value");
        }


        WikidataStatement.Rank rank = null;
        if (obj.has("rank")) {
            int i = obj.get("rank").getAsInt();
            if (i == 0) {
                rank = WikidataStatement.Rank.DEPRECATED;
            } else if (i == 1) {
                rank = WikidataStatement.Rank.NORMAL;
            } else if (i == 2) {
                rank = WikidataStatement.Rank.PREFERRED;
            } else {
                throw new WpParseException("unknown rank: " + i + " in " + jsonClaim);
            }
        }

        String uuid = obj.has("g") ? obj.get("g").getAsString() : null;

        // TODO: handle REFS in the 'refs' field

        // TODO: handle modifiers in the 'q' field

        return new WikidataStatement(uuid, item, prop, value, rank);
    }

    private WikidataEntity parseEntity(JsonElement value) throws WpParseException {
        WikidataEntity.Type entityType = null;
        int entityId = -1;
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            String s = array.get(0).getAsString();
            if (s.equals("item")) {
                entityType = WikidataEntity.Type.ITEM;
            } else if (s.equals("property")) {
                entityType = WikidataEntity.Type.PROPERTY;
            } else {
                throw new WpParseException("in parseEntity expected item or property, found " + value);
            }
            entityId = array.get(1).getAsInt();
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String id = value.getAsString().toLowerCase();
            if (id.startsWith("q")) {
                entityType = WikidataEntity.Type.ITEM;
                entityId = Integer.valueOf(id.substring(1));
            } else if (id.startsWith("p")) {
                entityType = WikidataEntity.Type.PROPERTY;
                entityId = Integer.valueOf(id.substring(1));
            } else {
                throw new WpParseException("Invalid entity id: " + id);
            }
        } else {
            throw new WpParseException("in parseEntity expected array, found " + value);
        }
        return new WikidataEntity(entityType, entityId);
    }

    private void parseAliases(WikidataEntity record, JsonElement value) throws WpParseException {
        if (value.isJsonArray() && value.getAsJsonArray().size() == 0) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonArray()) {
                if (validLanguage(entry.getKey())) {
                    JsonArray jsonVal = entry.getValue().getAsJsonArray();
                    List<String> values = new ArrayList<String>();
                    for (JsonElement obj : jsonVal) {
                        values.add(obj.getAsString());
                    }
                    record.getAliases().put(Language.getByLangCode(entry.getKey()), values);
                }
            } else if (entry.getValue().isJsonObject()) {
                if (validLanguage(entry.getKey())) {
                    JsonObject jsonVal = entry.getValue().getAsJsonObject();
                    List<String> values = new ArrayList<String>();
                    for (Map.Entry<String, JsonElement> entry2 : jsonVal.entrySet()) {
                        values.add(entry2.getValue().getAsString());
                    }
                    record.getAliases().put(Language.getByLangCode(entry.getKey()), values);
                }
            } else {
                LOG.severe("invalid alias: " + entry.getValue());
            }
        }
    }

    private void parseDescription(WikidataEntity record, JsonElement value) {
        if (value.isJsonArray() && value.getAsJsonArray().size() == 0) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            if (validLanguage(entry.getKey())) {
                record.getDescriptions().put(
                        Language.getByLangCode(entry.getKey()),
                        entry.getValue().getAsString());
            }
        }
    }

    private void parseLabels(WikidataEntity record, JsonElement value) {
        if (value.isJsonArray() && value.getAsJsonArray().size() == 0) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            if (validLanguage(entry.getKey())) {
                record.getLabels().put(
                        Language.getByLangCode(entry.getKey()),
                        entry.getValue().getAsString());
            }
        }
    }

    private WikidataValue parseValue(String type, JsonElement element) throws WpParseException {
        if (type.equals("string")) {
            return new WikidataValue(WikidataValue.Type.STRING, element.getAsString());
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
                        DateUtils.parseDate(time, "yyyyyyyyyyy-MM-dd'T'HH:mm:ss'Z'G")
                );
            } catch (ParseException e) {
                throw new WpParseException("Invalid date: " + element);
            }
        } else if (type.equals("wikibase-entityid")) {
            String entityType = element.getAsJsonObject().get("entity-type").getAsString();
            if (!entityType.equals("item")) {
                throw new WpParseException("unknown entity type: " + entityType + " in " + element);
            }
            return new WikidataValue(
                    WikidataValue.Type.ITEM,
                    element.getAsJsonObject().get("numeric-id").getAsInt()
            );
        } else if (Arrays.asList("globecoordinate").contains(type)) {
            return new WikidataValue(WikidataValue.Type.OTHER, type, element.toString());
        } else {
            throw new WpParseException("unknown wikidata type: " + type);
        }
    }

    private boolean validLanguage(String langCode) {
        return Language.hasLangCode(langCode) && langs.containsLanguage(langCode);
    }
}
