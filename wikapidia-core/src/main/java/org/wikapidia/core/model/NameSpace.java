package org.wikapidia.core.model;

import java.util.HashMap;

/**
 * A NameSpace defines the kind Wikipidia page a given page is.
 * The NameSpace contains the NameSpace enum that specifies what namespace a given NameSpace is in.
 * This allows us to query by NameSpace or by NameSpace and filter out the distinctions.
 */
public enum NameSpace {

    ARTICLE(0), DISAMBIG(0), TALK(1),
    USER(2), USER_TALK(3),
    WIKIPEDIA(4), WIKIPEDIA_TALK(5),
    FILE(6), FILE_TALK(7),
    MEDIA_WIKI(8), MEDIA_WIKI_TALK(9),
    TEMPLATE(10), TEMPLATE_TALK(11),
    HELP(12), HELP_TALK(13),
    CATEGORY(14), CATEGORY_TALK(15),
    PORTAL(100), PORTAL_TALK(101),
    BOOK(108), BOOK_TALK(109),
    EDUCATION_PROGRAM(446), EDUCATION_PROGRAM_TALK(447),
    TIMED_TEXT(710), TIMED_TEXT_TALK(711),
    MODULE(828), MODULE_TALK(829),

    SPECIAL(-1), MEDIA(-2);

    private short value;


    private NameSpace(int value) {
        this.value = (short)value;
    }

    /**
     * Every NameSpace has a corresponding Talk-type page.
     * @return This method will let you know if a given NameSpace is a Talk-type page.
     */
    public boolean isTalk() {
        return (getValue() >= 0 && getValue()%2 == 1);
    }

    /**
     * Returns a short ID for the NameSpace.
     * The ID is determined arbitrarily by this Enum, and should not be referenced to
     * anything else. It is unrelated to NameSpace.getValue().
     * @return
     */
    public short getArbitraryId() {
        return (short) ordinal();
    }

    /**
     *
     * @return The numeric value of the NameSpace as defined by Wikipedia.
     */
    public short getValue() {
        return value;
    }

    /**
     * Returns a namespace based on the arbitrary ID determined by the getArbitraryId method
     * @param id the arbitrary ID of the namespace
     * @return the namespace
     */
    public static NameSpace getNameSpaceById(int id) {
        return NameSpace.values()[id];
    }

    /**
     * Takes in a string and returns the correspond
     * @param s
     * @return null if the string does not match a namespace
     */
    public static NameSpace getNameSpaceByName(String s){
        s = s.toLowerCase();
        if (s.equals("")) return NameSpace.ARTICLE;
        else if(s.equals("talk")) return NameSpace.TALK;
        else if(s.equals("user")) return NameSpace.USER;
        else if(s.equals("user talk"))return NameSpace.USER_TALK;
        else if(s.equals("wikipedia")) return NameSpace.WIKIPEDIA;
        else if(s.equals("wp")) return NameSpace.WIKIPEDIA;
        else if(s.equals("project")) return NameSpace.WIKIPEDIA;
        else if(s.equals("wikipedia talk")) return NameSpace.WIKIPEDIA_TALK;
        else if(s.equals("wt")) return NameSpace.WIKIPEDIA_TALK;
        else if(s.equals("project talk")) return NameSpace.WIKIPEDIA_TALK;
        else if(s.equals("file"))return NameSpace.FILE;
        else if(s.equals("image"))return NameSpace.FILE;
        else if(s.equals("file talk"))return NameSpace.FILE_TALK;
        else if(s.equals("image talk"))return NameSpace.FILE_TALK;
        else if(s.equals("mediawiki"))return NameSpace.MEDIA_WIKI;
        else if(s.equals("mediawiki talk"))return NameSpace.MEDIA_WIKI_TALK;
        else if(s.equals("template"))return NameSpace.TEMPLATE;
        else if(s.equals("template talk"))return NameSpace.TEMPLATE_TALK;
        else if(s.equals("help"))return NameSpace.HELP;
        else if(s.equals("help talk"))return NameSpace.HELP_TALK;
        else if(s.equals("category"))return NameSpace.CATEGORY;
        else if(s.equals("category talk"))return NameSpace.CATEGORY_TALK;
        else if(s.equals("portal"))return NameSpace.PORTAL;
        else if(s.equals("portal talk"))return NameSpace.PORTAL_TALK;
        else if(s.equals("book"))return NameSpace.BOOK;
        else if(s.equals("book talk"))return NameSpace.BOOK_TALK;
        else if(s.equals("education program"))return NameSpace.EDUCATION_PROGRAM;
        else if(s.equals("education program talk"))return NameSpace.EDUCATION_PROGRAM_TALK;
        else if(s.equals("timedtext"))return NameSpace.TIMED_TEXT;
        else if(s.equals("timedtext talk"))return NameSpace.TIMED_TEXT_TALK;
        else if(s.equals("module"))return NameSpace.MODULE;
        else if(s.equals("module talk"))return NameSpace.MODULE_TALK;
        else if(s.equals("special"))return NameSpace.SPECIAL;
        else if(s.equals("media"))return NameSpace.MEDIA;
        else return null;
    }

    /**
     * Returns a namespace based on the value of that namespace defined by Wikipedia.
     * Never returns a disambiguation.
     * @param value the numeric value of the NameSpace as defined by Wikipedia
     * @return the corresponding NameSpace if it exists, else null
     */
    public static NameSpace getNameSpaceByValue(int value){
        if(value == 0) return NameSpace.ARTICLE;
        for (NameSpace v : NameSpace.values()){
            if (value == v.getValue()) {return v;}
        }
        return null;
    }
}
