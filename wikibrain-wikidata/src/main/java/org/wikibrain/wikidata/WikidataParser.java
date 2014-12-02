package org.wikibrain.wikidata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.WpParseException;
import org.wikibrain.utils.WpIOUtils;
import org.wikidata.wdtk.datamodel.interfaces.*;
import org.wikidata.wdtk.datamodel.json.ValueJsonConverter;
import org.wikidata.wdtk.datamodel.json.jackson.*;
import org.wikidata.wdtk.datamodel.json.jackson.datavalues.JacksonValue;
import org.wikidata.wdtk.dumpfiles.MwRevision;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class WikidataParser {
    private static final Logger LOG = Logger.getLogger(WikidataParser.class.getName());
    private final LanguageSet langs;
    final ObjectMapper mapper = new ObjectMapper();

    public WikidataParser() {
        this(LanguageSet.ALL);
    }

    public WikidataParser(LanguageSet langs) {
        this.langs = langs;
    }

    private static BufferedWriter writer;

    public WikidataEntity parse(String json) throws WpParseException {
        JacksonTermedDocument mwDoc;

        try {
            mwDoc = mapper.readValue(json, JacksonTermedDocument.class);
        } catch (IOException e) {
            LOG.info("Error parsing: " + json);
            throw new WpParseException(e);
        }

        WikidataEntity record = new WikidataEntity(mwDoc.getEntityId().getId());

        // Aliases (multiple per language)
        for (List<MonolingualTextValue> vlist : mwDoc.getAliases().values()) {
            if (vlist.isEmpty()) continue;
            if (!validLanguage(vlist.get(0).getLanguageCode())) continue;
            Language lang = Language.getByLangCodeLenient(vlist.get(0).getLanguageCode());
            record.getAliases().put(lang, new ArrayList<String>());
            for (MonolingualTextValue v : vlist) {
                record.getAliases().get(lang).add(v.getText());
            }
        }

        // Descriptions (one per language)
        for (MonolingualTextValue v : mwDoc.getDescriptions().values()) {
            if (validLanguage(v.getLanguageCode())) {
                Language lang = Language.getByLangCodeLenient(v.getLanguageCode());
                record.getDescriptions().put(lang, v.getText());
            }
        }

        // Labels (one per language)
        for (MonolingualTextValue v : mwDoc.getLabels().values()) {
            if (validLanguage(v.getLanguageCode())) {
                Language lang = Language.getByLangCodeLenient(v.getLanguageCode());
                record.getLabels().put(lang, v.getText());
            }
        }

        // Claims (only for Item entities)
        if (mwDoc instanceof JacksonItemDocument) {
            for (List<JacksonStatement> statements : ((JacksonItemDocument)mwDoc).getJsonClaims().values()) {
                for (JacksonStatement s : statements) {
                    record.getStatements().add(parseOneClaim(record, s));
                }
            }
        }

        return record;
    }

    private WikidataStatement parseOneClaim(WikidataEntity item, JacksonStatement js) throws WpParseException {
        String propId =js.getMainsnak().getProperty();  // e.g. "P34"
        WikidataEntity prop = new WikidataEntity(WikidataEntity.Type.PROPERTY, Integer.valueOf(propId.substring(1)));
        String valTypeStr = js.getMainsnak().getSnaktype();
        JsonElement jsonVal = null;

        WikidataValue value;
        if (valTypeStr.equals("value")) { // more specific type available
            JacksonValueSnak snak = (JacksonValueSnak)js.getMainsnak();
            valTypeStr = ((JacksonValue)snak.getValue()).getType();
            value = snakToValue(valTypeStr, snak.getValue());
        } else {
            value = jsonToValue(valTypeStr, jsonVal);
        }

        WikidataStatement.Rank rank;
        if (js.getRank() == null) {
            rank = null;
        } else if (js.getRank() == StatementRank.PREFERRED) {
            rank = WikidataStatement.Rank.PREFERRED;
        } else if (js.getRank() == StatementRank.NORMAL) {
            rank = WikidataStatement.Rank.NORMAL;
        } else if (js.getRank() == StatementRank.DEPRECATED) {
            rank = WikidataStatement.Rank.DEPRECATED;
        } else {
            throw new WpParseException("unknown rank: " + js.getRank() + " in " + js);
        }

        String uuid = js.getStatementId();

        return new WikidataStatement(uuid, item, prop, value, rank);
    }

    public WikidataValue snakToValue(final String type, Value snak) {
        String jsonStr = snak.accept(new ValueJsonConverter()).toString();
        final JsonElement element = new JsonParser().parse(jsonStr);
        WikidataValue val = snak.accept(new ValueVisitor<WikidataValue>() {

            @Override
            public WikidataValue visit(EntityIdValue value) {
                if (value.getEntityType().equals(EntityIdValue.ET_ITEM)) {
                    return WikidataValue.forItem(Integer.valueOf(value.getId().substring(1)));
                } else if (value.getEntityType().equals(EntityIdValue.ET_PROPERTY)) {
                    throw new IllegalArgumentException("Did not expect entity property");
                } else {
                    throw new IllegalArgumentException("Unknown entity type: " + value.getEntityType());
                }
            }

            @Override
            public WikidataValue visit(GlobeCoordinatesValue value) {
                return new WikidataValue(type, gsonToPrimitive(element), element);
            }

            @Override
            public WikidataValue visit(QuantityValue value) {
                return new WikidataValue(type, gsonToPrimitive(element), element);
            }

            @Override
            public WikidataValue visit(StringValue value) {
                return WikidataValue.forString(value.getString());
            }

            @Override
            public WikidataValue visit(TimeValue value) {
                Calendar c = new GregorianCalendar(
                        ((int)value.getYear()), value.getMonth()-1, value.getDay(),
                        value.getHour(), value.getMinute(), value.getSecond());
                return new WikidataValue(
                        WikidataValue.Type.TIME,
                        c.getTime(),
                        element
                );
            }

            @Override
            public WikidataValue visit(MonolingualTextValue value) {
                return WikidataValue.forString(value.getText());
            }

            @Override
            public WikidataValue visit(DatatypeIdValue value) { throw new IllegalArgumentException(); }

        });
        return val;
    }


    public WikidataValue jsonToValue(String type, JsonElement element) throws WpParseException {
        if (type.equals("somevalue")) {
            return new WikidataValue(WikidataValue.Type.SOMEVALUE, null, JsonNull.INSTANCE);
        } else if (type.equals("novalue")) {
            return new WikidataValue(WikidataValue.Type.NOVALUE, null, JsonNull.INSTANCE);
        }

        String fullJson = "{ \"type\" : " + type + ", \"value\" : " + element.toString() + " }";
        System.out.println("fullJson is " + fullJson);
        try {
            Value snak = mapper.readValue(fullJson, JacksonValue.class);
            return snakToValue(type, snak);
        } catch (IOException e) {
            throw new WpParseException(e);
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
                return prim.getAsNumber();
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

    private boolean validLanguage(String langCode) {
        return Language.hasLangCode(langCode) && langs.containsLanguage(langCode);
    }
}
