package org.wikapidia.wikidata;

import com.google.gson.*;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.WpParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class WikidataParser {
    private static final Logger LOG = Logger.getLogger(WikidataParser.class.getName());

    public WikidataRawRecord parse(RawPage rawPage) throws WpParseException {
        WikidataRawRecord record = new WikidataRawRecord(rawPage);
        JsonParser parser = new JsonParser();

        JsonObject obj;
        try {
            obj = parser.parse(rawPage.getBody()).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            System.err.println("invalid page: " + rawPage.getBody());
            throw new WpParseException(e);
        }

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
                parseEntity(record, fields.getValue());
            } else if (name.equals("claims")) {
                parseClaims(record, fields.getValue());
            } else {
                LOG.log(Level.WARNING, "unexpected field '" + name + "' when parsing " + rawPage.getTitle());
            }
        }

        return record;
    }

    private void parseClaims(WikidataRawRecord record, JsonElement value) throws WpParseException {
    }

    private void parseEntity(WikidataRawRecord record, JsonElement value) throws WpParseException {
        JsonArray array = value.getAsJsonArray();
        record.setEntityType(array.get(0).getAsString());
        record.setEntityId(array.get(1).getAsInt());
    }

    private void parseAliases(WikidataRawRecord record, JsonElement value) throws WpParseException {
        if (value.isJsonArray() && value.getAsJsonArray().size() == 0) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonArray()) {
                JsonArray jsonVal = entry.getValue().getAsJsonArray();
                List<String> values = new ArrayList<String>();
                for (JsonElement obj : jsonVal) {
                    values.add(obj.getAsString());
                }
                record.getAliases().put(entry.getKey(), values);
            } else if (entry.getValue().isJsonObject()) {
                JsonObject jsonVal = entry.getValue().getAsJsonObject();
                List<String> values = new ArrayList<String>();
                for (Map.Entry<String, JsonElement> entry2 : jsonVal.entrySet()) {
                    values.add(entry2.getValue().getAsString());
                }
                record.getAliases().put(entry.getKey(), values);
            } else {
                LOG.severe("invalid alias for " + record.getRawPage().getTitle() + ": " + entry.getValue());
            }
        }
    }

    private void parseDescription(WikidataRawRecord record, JsonElement value) {
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            record.getDescriptions().put(entry.getKey(), entry.getValue().getAsString());
        }
    }

    private void parseLabels(WikidataRawRecord record, JsonElement value) {
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            record.getLabels().put(entry.getKey(), entry.getValue().getAsString());
        }
    }
}
