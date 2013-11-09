package org.wikapidia.core.dao.live;

/**
 * A QueryReply class to handle json objects we got from live wiki API for a LocalArticle object
 * @author Toby "Jiajun" Li
 */

public class LocalArticleQueryReply extends LocalPageQueryReply {
    public LocalArticleQueryReply(String text){
        super(text);
    }
}
