package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: research
 * Date: 6/7/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class UniversalPage {

    protected final int univId;
    protected final Multimap<Language, LocalPage> localPages;

    protected UniversalPage(int univId, Multimap<Language, LocalPage> localPages) {
        this.univId = univId;
        this.localPages = localPages;
    }

    //other constructors?

    public boolean isInLanguageSet(LanguageSet ls, boolean mustBeInAllLangs){
        for (Integer langId : ls.getLangIds()){
            boolean isInLang = isInLanguage(langId);
            if (isInLang && !mustBeInAllLangs){
                return true;
            }
            if (!isInLang && mustBeInAllLangs){
                return false;
            }
        }
        return true;
    }

    public int getNumberOfLanguages(){
        return localPages.keySet().size();
    }

    public int getNumLocalConcepts(LanguageSet ls){
        int counter = 0;
        for (int langId : ls.getLangIds()){
            List<LocalConcept> curLCs = getLocalConcepts(langId);
            if (curLCs != null){
                counter+=curLCs.size();
            }
        }
        return counter;
    }
}
