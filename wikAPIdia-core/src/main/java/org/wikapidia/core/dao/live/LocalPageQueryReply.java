package org.wikapidia.core.dao.live;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.util.Map;
import java.util.Set;
/**
 * Created with IntelliJ IDEA.
 * User: Toby "Jiajun" Li
 * Date: 10/27/13
 * Time: 9:13 PM
 * To change this template use File | Settings | File Templates.
 */


public class LocalPageQueryReply {



    private Long pageId;
    private String title;
    private String pageLanguage;
    private Long nameSpace;
    private boolean isRedirect;
    private boolean isDisambig;

    /** Construct a LocalPageQueryReply (A object used to describe a local page we fetched from wiki server) from a JSON object
     * @param text A JSON object we got from wiki server
     */
    LocalPageQueryReply(String text){
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
    /**
     *
     * @return An Integer: the pageId of this page
     */
    public int getId(){
        return pageId.intValue();
    }
    /**
     *
     * @return A Title: the title of this page
     */
    public Title getTitle(){
        return new Title(title, Language.getByLangCode(pageLanguage));
    }
    /**
     *
     * @return A NameSpace: the namespace of this page
     */
    public NameSpace getNameSpace(){
        return NameSpace.getNameSpaceByArbitraryId(nameSpace.intValue());
    }
    /**
     *
     * @return A boolean: whether this page is redirect
     */
    public boolean isRedirect(){
        return isRedirect;
    }
    /**
     *
     * @return A boolean: whether this page is disambiguous
     */
    public boolean isDisambig(){
        return isDisambig;
    }





}
