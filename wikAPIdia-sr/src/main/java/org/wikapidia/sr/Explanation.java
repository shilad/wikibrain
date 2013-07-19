package org.wikapidia.sr;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.core.model.LocalPage;

import java.util.List;

public class Explanation {
    private String format;
    private List<Object> information;

    /**
     * Used to make a human-readable explanations of sr measures
     * @param format a string with ?s where the objects should be placed
     * @param information the objects to be placed in the format string
     */
    public Explanation(String format, List information){
        this.format = format;
        this.information = information;
    }

    /**
     *
     * @return an explanation string using the titles of the information
     */
    public String getPlaintext(){
        if (information ==null|| information.isEmpty()){
            return format;
        }
        String[] plaintextBuilder = format.split("\\?",-1);
        String plaintext = plaintextBuilder[0];
        for (int i = 1; i<plaintextBuilder.length; i++){
            Object object = information.get(i-1);

            //Handle the different possible types of information.
            //Add additional handlers as appropriate
            if (object instanceof LocalPage){
                plaintext+=((LocalPage) object).getTitle().getCanonicalTitle();
            } else if (object instanceof UniversalPage){
                Language language = ((UniversalPage) object).getLanguageSet().getDefaultLanguage();
                plaintext+=((LocalPage)((UniversalPage) object).getLocalPages(language).toArray()[0]).getTitle().getCanonicalTitle();
            }else {
                plaintext+=object.toString();
            }

            plaintext+=plaintextBuilder[i];
        }
        return plaintext;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<Object> getInformation() {
        return information;
    }

    public void setInformation(List<Object> information) {
        this.information = information;
    }
}
