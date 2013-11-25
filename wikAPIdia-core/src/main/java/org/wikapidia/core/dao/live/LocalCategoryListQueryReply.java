package org.wikapidia.core.dao.live;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.model.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A QueryReply class to handle json objects we got from live wiki API for a list of categories
 * @author Toby "Jiajun" Li
 */
public class LocalCategoryListQueryReply extends QueryReply {
    public List<LocalCategory> categoryList = new ArrayList<LocalCategory>();

    public LocalCategoryListQueryReply(String text, Language language) throws ConfigurationException {
        Gson gson = new Gson();
        LocalCategoryDao localCategoryDao = new Configurator(new Configuration()).get(LocalCategoryDao.class, "live");
        JsonParser jp = new JsonParser();
        JsonObject test = jp.parse(text).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> pageSet = jp.parse(text).getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().entrySet();

        for (Map.Entry<String, JsonElement> entry: pageSet)
        {
            Integer pageId = entry.getValue().getAsJsonObject().get("pageid").getAsInt();
            String title = entry.getValue().getAsJsonObject().get("title").getAsString();

            Integer nameSpace = entry.getValue().getAsJsonObject().get("ns").getAsInt();
            for(JsonElement elem: entry.getValue().getAsJsonObject().get("categories").getAsJsonArray()){

                try{
                    Title categoryTitle = new Title( elem.getAsJsonObject().get("title").getAsString(), language);
                    categoryList.add(localCategoryDao.getByTitle(language, categoryTitle));
                }
                catch(Exception e){

                }

            }
            this.pageId = pageId;
            this.title = title;
            this.pageLanguage = pageLanguage;
            this.nameSpace = nameSpace;

        }

    }

    /**
     *
     * @return a list of categories that an article beIntegers to
     */
    public List<LocalCategory> getCategoryList(){
        return categoryList;
    }


}



