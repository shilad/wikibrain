package org.wikapidia.sr;

import org.wikapidia.core.model.LocalPage;

import java.util.List;

public class Explanation {
    private String format;
    private List<LocalPage> pages;

    /**
     * Used to make a human-readable explanations of sr measures
     * @param format a string with ?s where the objects should be placed
     * @param pages
     */
    public Explanation(String format, List pages){
        this.format = format;
        this.pages = pages;
    }

    /**
     *
     * @return an explanation string using the titles of the pages
     */
    public String getPlaintext(){
        String[] plaintextBuilder = format.split("\\?",-1);
        String plaintext = plaintextBuilder[0];
        for (int i = 1; i<plaintextBuilder.length; i++){
            plaintext+=pages.get(i-1).getTitle().getCanonicalTitle()+plaintextBuilder[i];
        }
        return plaintext;
    }
}
