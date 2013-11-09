package org.wikapidia.core.dao.live;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.Set;

/**
 * A QueryReply class to handle json objects we got from live wiki API for a LocalCategory object
 * @author Toby "Jiajun" Li
 */
public class LocalCategoryQueryReply extends LocalPageQueryReply {
    public LocalCategoryQueryReply(String text){
        super(text);
    }

}
