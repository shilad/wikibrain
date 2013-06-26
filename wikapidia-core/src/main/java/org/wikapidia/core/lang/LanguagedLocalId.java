package org.wikapidia.core.lang;

/**
 * Created with IntelliJ IDEA.
 * User: bjhecht
 * Date: 6/26/13
 * Time: 3:54 PM
 * Encapsulates the idea of a <Language, Local Id> pair. Useful when a LocalPage is not needed or
 * when getting a LocalPage is expensive.
 */
public class LanguagedLocalId {

    private final int localId;
    private final Language language;

    public LanguagedLocalId(int localId, Language language){
        this.localId = localId;
        this.language = language;
    }

    public int getLocalId(){
        return localId;
    }

    public Language getLanguage(){
        return language;
    }

    @Override
    public boolean equals(Object o){

        if (o instanceof LanguagedLocalId){
            LanguagedLocalId input = (LanguagedLocalId)o;
            return (this.localId == input.getLocalId() && this.language.equals(input.language));
        }else{
            return false;
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(localId);
        sb.append("_");
        sb.append(language.getLangCode());
        return sb.toString();
    }
}
