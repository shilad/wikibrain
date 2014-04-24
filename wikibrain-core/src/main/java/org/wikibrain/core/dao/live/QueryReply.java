package org.wikibrain.core.dao.live;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.*;

/**
 * An abstract class used to store information of interest for a page of a query result
 * Also contains methods to construct a wikibrain core object from the information contained here
 * @author Toby "Jiajun" Li and derian
 */

public class QueryReply {


    public Integer pageId;
    public String title;
    public Integer nameSpace;
    public Boolean isRedirect;
    public Boolean isDisambig;


    public QueryReply(int pageId, String title, int nameSpace, boolean isRedirect, boolean isDisambig) {
        this.pageId = pageId;
        this.title = title;
        this.nameSpace = nameSpace;
        this.isRedirect = isRedirect;
        this.isDisambig = isDisambig;
    }

    public LocalLink getLocalOutLink(Language lang, int sourceId) {
        return new LocalLink(lang, null, sourceId, pageId, true, -1, null, null);
    }

    public LocalLink getLocalInLink(Language lang, int destId) {
        return new LocalLink(lang, null, pageId, destId, false, -1, null, null);
    }

    public LocalPage getLocalPage(Language lang) {
        return new LocalPage(lang, pageId, this.getTitle(lang), this.getNameSpace(), isRedirect, isDisambig);
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
        return NameSpace.getNameSpaceByValue(nameSpace.intValue());
    }
}
