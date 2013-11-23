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
    public List<Integer> categories;
    public List<Integer> categoryMembers;

    public QueryReply() {

    }

    public QueryReply(int pageId, String title, int nameSpace, boolean isRedirect, boolean isDisambig, List<Integer> categories, List<Integer> categoryMembers) {
        this.pageId = pageId;
        this.title = title;
        this.nameSpace = nameSpace;
        this.isRedirect = isRedirect;
        this.isDisambig = isDisambig;
        this.categories = categories;
        this.categoryMembers = categoryMembers;
    }

    public LocalLink getLocalLink(Language lang, int sourceId, boolean outlink) {
        return new LocalLink(lang, title, sourceId, pageId, outlink, -1, true, null);
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
