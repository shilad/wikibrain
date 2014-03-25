package org.wikapidia.spatial.core;

import org.wikapidia.core.lang.Language;

/**
 * Created by toby on 3/25/14.
 */
public class SpatialGeomName {

    private final String geomName;
    private final String langCode;

    public SpatialGeomName(String geomName, String langCode){
        this.geomName = geomName;
        this.langCode = langCode;
    }

    public SpatialGeomName(String geomName, Language language){
        this.geomName = geomName;
        this.langCode = language.getLangCode();
    }

    public SpatialGeomName(String geomName, Integer langId){
        this.geomName = geomName;
        this.langCode = Language.getById(langId).getLangCode();
    }

    public String getGeomName(){
        return geomName;
    }

    public String getLangCode(){
        return langCode;
    }

    public Language getLang(){
        return Language.getByLangCode(langCode);
    }

    public Integer getLangId(){
        return new Integer(Language.getByLangCode(langCode).getId());
    }





}
