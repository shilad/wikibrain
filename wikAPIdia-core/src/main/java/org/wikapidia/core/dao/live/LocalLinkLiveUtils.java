package org.wikapidia.core.dao.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/3/13
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalLinkLiveUtils {
    /**
     * Get a list of links from JSON text
     * @param text
     * @param language
     * @param sourceId
     * @param outlinks
     * @return
     */
    public static Iterable<LocalLink> parseLinks(String text, Language language, int sourceId, boolean outlinks) throws DaoException {
        JsonParser jp = new JsonParser();
        String linkType = (outlinks ? "pages" : "backlinks");
        JsonObject jo = jp.parse(text).getAsJsonObject().get("query").getAsJsonObject();
        if (jo.get(linkType).isJsonObject()) {
            return getLinksFromJsonObject(jo.get(linkType).getAsJsonObject(), language, sourceId, outlinks);
        }
        else if (jo.get(linkType).isJsonArray()) {
            return getLinksFromJsonArray(jo.get(linkType).getAsJsonArray(), language, sourceId, outlinks);
        }
        else {
            throw new DaoException("Ill-formed links JSON can't be parsed");
        }
    }

    private static Iterable<LocalLink> getLinksFromJsonObject(JsonObject jo, Language language, int sourceId, boolean outlinks) {
        List<LocalLink> links = new ArrayList<LocalLink>();
        Set<Map.Entry<String, JsonElement>> linkSet = jo.entrySet();

        for (Map.Entry<String, JsonElement> entry: linkSet) {
            String anchorText = entry.getValue().getAsJsonObject().get("title").getAsString();
            Integer destId = entry.getValue().getAsJsonObject().get("pageid").getAsInt();

            /*location, isParesable, and locType can't be retrieved from wiki server, so default values
             *of -1, true, and null, respectively, are used to create the link*/
            LocalLink link = new LocalLink(language, anchorText, sourceId, destId, outlinks, -1, true, null);
            links.add(link);
        }

        return links;
    }

    private static Iterable<LocalLink> getLinksFromJsonArray(JsonArray linkArray, Language language, int sourceId, boolean outlinks) {
        List<LocalLink> links = new ArrayList<LocalLink>();

        for (JsonElement linkElem : linkArray) {
            String anchorText = linkElem.getAsJsonObject().get("title").getAsString();
            Integer destId = linkElem.getAsJsonObject().get("pageid").getAsInt();

            /*location, isParesable, and locType can't be retrieved from wiki server, so default values
             *of -1, true, and null, respectively, are used to create the link*/
            LocalLink link = new LocalLink(language, anchorText, sourceId, destId, outlinks, -1, true, null);
            links.add(link);
        }

        return links;
    }
}
