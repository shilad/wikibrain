package org.wikapidia.core.dao.live;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.dao.live.QueryReply;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
/**
 * A QueryReply class to handle json objects we got from live wiki API for a LocalPage object
 * @author Toby "Jiajun" Li
 */
public class LocalPageQueryReply extends QueryReply {
    public boolean isRedirect;
    public boolean isDisambig;

    /**Constructor: Construct a LocalPageQueryReply from the reply from wiki server
     *
     * @param text Query reply from wiki server
     */
    public LocalPageQueryReply(String text){

        Gson gson = new Gson();
        JsonParser jp = new JsonParser();
        JsonObject test = jp.parse(text).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> pageSet = jp.parse(text).getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry: pageSet)
        {
            Long pageId = entry.getValue().getAsJsonObject().get("pageid").getAsLong();
            String title = entry.getValue().getAsJsonObject().get("title").getAsString();
            String pageLanguage = entry.getValue().getAsJsonObject().get("pagelanguage").getAsString();
            Long nameSpace = entry.getValue().getAsJsonObject().get("ns").getAsLong();
            Boolean isRedirect = entry.getValue().getAsJsonObject().has("redirect");
            Boolean isDisambig = entry.getValue().getAsJsonObject().get("title").getAsString().contains("(disambiguation)");
            this.pageId = pageId;
            this.title = title;
            this.pageLanguage = pageLanguage;
            this.nameSpace = nameSpace;
            this.isRedirect = isRedirect;
            this.isDisambig = isDisambig;
        }
    }

    public LocalPageQueryReply(){

    }
    /**
     *
     * @return a boolean: Whether this page is a redirect page
     */
    public boolean isRedirect(){
        return isRedirect;
    }

    /**
     *
     * @return a boolean: Whether this page is a disambiguous page
     */
    public boolean isDisambig(){
        return isDisambig;
    }

    /**
     *
     * @return the namespace of this page

     */
    public NameSpace getNameSpace(){
        return NameSpace.getNameSpaceByArbitraryId(nameSpace.intValue());
    }

}