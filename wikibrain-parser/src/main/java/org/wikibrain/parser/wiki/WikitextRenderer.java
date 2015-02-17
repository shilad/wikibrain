package org.wikibrain.parser.wiki;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.live.QueryParser;
import org.wikibrain.core.lang.Language;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class WikitextRenderer {
    public String renderPage(Language language, String title) throws DaoException {
        String url = makeUrl(language, "page", title, "prop", "text");
        JsonObject obj = fetchURL(url);
        return obj.get("text").getAsJsonObject().get("*").getAsString();
    }

    public List<String> extractExternalLinks(Language language, String title) throws DaoException {
        String url = makeUrl(language, "page", title, "prop", "externallinks");
        JsonObject obj = fetchURL(url);
        List<String> links = new ArrayList<String>();
        for (JsonElement e :  obj.get("externallinks").getAsJsonArray()) {
            links.add(e.getAsString());
        }
        return links;
    }

    public String makeUrl(Language language, String ... props) {
        String url = "http://" + language.getDomain() + "/w/api.php?action=parse";
        for (int i = 0; i < props.length; i += 2) {
            String key = props[i];
            String val = props[i+1];
            url += "&" + URLEncoder.encode(key) + "=" + URLEncoder.encode(val);
        }
        url += "&format=json";
        return url;
    }

    /**
     * queries the wikipedia server for text output that can be parsed to create a wikibrain data object
     * sets the class attribute queryResult to the value of this raw output
     * @return
     * @throws org.wikibrain.core.dao.DaoException
     */
    private JsonObject fetchURL(String url) throws DaoException {
        InputStream inputStr;

        try{
            inputStr = new URL(url).openStream();
            try {
                String body = IOUtils.toString(inputStr);
                QueryParser parser = new QueryParser();
                return parser.parseQueryObject(body, "parse");
            }
            catch(Exception e){
                throw new DaoException("Error parsing LiveDao query URL");
            }
            finally {
                IOUtils.closeQuietly(inputStr);
            }
        }
        catch(Exception e){
            throw new DaoException("Error getting page from the Wikipedia Server (Check your internet connection) ");
        }
    }

    public static void main(String args[]) throws DaoException {
        WikitextRenderer renderer = new WikitextRenderer();
        System.out.println(renderer.extractExternalLinks(Language.EN, "Barack Obama"));
//        System.out.println(renderer.renderPage(Language.EN, "Barack Obama"));
    }
}
