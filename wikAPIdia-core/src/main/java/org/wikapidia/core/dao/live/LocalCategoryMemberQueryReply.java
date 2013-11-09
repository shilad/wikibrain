package org.wikapidia.core.dao.live;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.model.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A QueryReply class to handle json objects we got from live wiki API for a list of category members
 * @author Toby "Jiajun" Li
 */
public class LocalCategoryMemberQueryReply extends QueryReply {


    public List<LocalCategoryMember> memberList = new ArrayList<LocalCategoryMember>();

    public LocalCategoryMemberQueryReply(String text, Integer localCategoryId, Language language) throws DaoException{
        Gson gson = new Gson();
        JsonParser jp = new JsonParser();
        //JsonObject test = jp.parse(text).getAsJsonObject();

        for(JsonElement elem: jp.parse(text).getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray()){
            try{
                memberList.add(new LocalCategoryMember(localCategoryId, elem.getAsJsonObject().get("pageid").getAsInt(), language));
            }
            catch (Exception e){
                throw new DaoException("Failed to parse category members list");
            }


        }
            this.pageId = pageId;
            this.title = title;
            this.pageLanguage = pageLanguage;
            this.nameSpace = nameSpace;

    }



    /**
     *
     * @return the list of category members
     */
    public List<LocalCategoryMember> getLocalCategoryMemberList(){
        return memberList;
    }


}
