package org.wikapidia.core.dao.live;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * An abstract class used to handle json objects we got from live wiki API
 * @author Toby "Jiajun" Li
 */

public class QueryReply {


    public Integer pageId;
    public String title;
    public String pageLanguage;
    public Integer nameSpace;
    public Boolean isRedirect;
    public Boolean isDisambig;
    //public List<Integer> categories;
    //public List<Integer> categoryMembers;

    public QueryReply() {

    }

    public QueryReply(int pageId, String title, int nameSpace, boolean isRedirect, boolean isDisambig) {
        this.pageId = pageId;
        this.title = title;
        this.nameSpace = nameSpace;
        this.isRedirect = isRedirect;
        this.isDisambig = isDisambig;
    }

    public LocalLink getLocalOutLink(Language lang, int sourceId) {
        return new LocalLink(lang, title, sourceId, pageId, true, -1, true, null);
    }

    public LocalLink getLocalInLink(Language lang, int destId) {
        return new LocalLink(lang, title, pageId, destId, false, -1, true, null);
    }

    public LocalPage getLocalPage(Language lang) {
        return new LocalPage(lang, pageId, this.getTitle(lang), this.getNameSpace(), isRedirect, isDisambig);
    }

    public LocalCategoryMember getLocalCategoryMember(int categoryId, Language lang) {
        return new LocalCategoryMember(categoryId, pageId, lang);
    }

    public int getId() {
        return pageId;
    }

    /**
     *
     * @return A Title: the title of this page
     */
    public Title getTitle(Language lang){
        return new Title(title, lang);
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
