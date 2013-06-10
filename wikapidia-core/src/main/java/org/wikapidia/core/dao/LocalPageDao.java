package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class LocalPageDao<T extends LocalPage> {

    public abstract T getByPageId(Language language, int pageId);

    public abstract T getByTitle(Language language, Title title, PageType ns);

    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) {
        Map<Integer, T> map = new HashMap<Integer,T>();
        for (int id : pageIds){
            map.put(id,getByPageId(language,id));
        }
        return map;
    }

    public Map<Title, T> getbyTitles(Language language, Collection<Title> titles, PageType ns){
        Map<Title, T> map = new HashMap<Title, T>();
        for (Title title : titles){
            map.put(title, getByTitle(language, title, ns));
        }
        return map;
    }
}
