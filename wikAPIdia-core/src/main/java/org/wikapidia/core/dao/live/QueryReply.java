package org.wikapidia.core.dao.live;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
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


public abstract class QueryReply {



    public Long pageId;
    public String title;
    public String pageLanguage;
    public Long nameSpace;



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


}
