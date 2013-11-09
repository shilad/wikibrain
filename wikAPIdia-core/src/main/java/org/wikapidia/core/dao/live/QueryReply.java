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
 * An abstract class used to handle json objects we got from live wiki API
 * @author Toby "Jiajun" Li
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
