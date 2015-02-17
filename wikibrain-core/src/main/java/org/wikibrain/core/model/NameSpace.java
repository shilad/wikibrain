package org.wikibrain.core.model;

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
     * You probably don't want to use this. Use getNameSpaceByValue instead.
     *
     * @param id the arbitrary ID of the namespace
     * @return the namespace
     */
    public static NameSpace getNameSpaceByArbitraryId(int id) {
        return NameSpace.values()[id];
    }

    /**
     * Takes in a string and returns the correspond
     * @param s
     * @return null if the string does not match a namespace
     */
    public static NameSpace getNameSpaceByName(String s){
        s=s.toUpperCase();
        for (NameSpace ns : NameSpace.values()){
            if (ns.toString().replace("_"," ").equals(s)){
                return ns;
            }
        }
        if (s.equals("")) return NameSpace.ARTICLE;
        else if (s.equals("WP")) return NameSpace.WIKIPEDIA;
        else if (s.equals("WT")) return NameSpace.WIKIPEDIA_TALK;
        else if (s.equals("IMAGE")) return NameSpace.FILE;
        else if (s.equals("IMAGE TALK")) return NameSpace.FILE_TALK;
        else if (s.equals("PROJECT")) return NameSpace.WIKIPEDIA;
        else if (s.equals("PROJECT TALK")) return NameSpace.WIKIPEDIA_TALK;
        else if (s.equals("MEDIAWIKI")) return NameSpace.MEDIA_WIKI;
        else if (s.equals("MEDIAWIKI TALK")) return NameSpace.MEDIA_WIKI_TALK;
        else if (s.equals("CAT")) return NameSpace.CATEGORY;
        else if (s.equals("MOS")) return NameSpace.WIKIPEDIA;
        else if (s.equals("H")) return NameSpace.HELP;
        else if (s.equals("P")) return NameSpace.PORTAL;
        else if (s.equals("T")) return NameSpace.TALK;
        else return null;
    }

    public static boolean isNamespaceString(String s){
        return !(getNameSpaceByName(s)==null);
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
