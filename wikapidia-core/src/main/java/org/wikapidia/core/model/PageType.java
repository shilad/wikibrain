package org.wikapidia.core.model;

/**
 * A PageType defines the kind Wikipidia page a given page is.
 * The PageType contains the NameSpace enum that specifies what namespace a given PageType is in.
 * This allows us to query by NameSpace or by PageType and filter out the distinctions.
 */
public enum PageType {

    ARTICLE(NameSpace.MAIN), REDIRECT(NameSpace.MAIN), DISAMBIG(NameSpace.MAIN),
    TALK(NameSpace.TALK),
    USER(NameSpace.USER),
    USER_TALK(NameSpace.USER_TALK),
    WIKIPEDIA(NameSpace.WIKIPEDIA),
    WIKIPEDIA_TALK(NameSpace.WIKIPEDIA_TALK),
    FILE(NameSpace.FILE),
    FILE_TALK(NameSpace.FILE_TALK),
    MEDIA_WIKI(NameSpace.MEDIA_WIKI),
    MEDIA_WIKI_TALK(NameSpace.MEDIA_WIKI_TALK),
    TEMPLATE(NameSpace.TEMPLATE),
    TEMPLATE_TALK(NameSpace.TEMPLATE_TALK),
    HELP(NameSpace.HELP),
    HELP_TALK(NameSpace.HELP_TALK),
    CATEGORY(NameSpace.CATEGORY),
    CATEGORY_TALK(NameSpace.CATEGORY_TALK),
    PORTAL(NameSpace.PORTAL),
    PORTAL_TALK(NameSpace.PORTAL_TALK),
    BOOK(NameSpace.BOOK),
    BOOK_TALK(NameSpace.BOOK_TALK),
    EDUCATION_PROGRAM(NameSpace.EDUCATION_PROGRAM),
    EDUCATION_PROGRAM_TALK(NameSpace.EDUCATION_PROGRAM_TALK),
    TIMED_TEXT(NameSpace.TIMED_TEXT),
    TIMED_TEXT_TALK(NameSpace.TIMED_TEXT_TALK),
    MODULE(NameSpace.MODULE),
    MODULE_TALK(NameSpace.MODULE_TALK),

    SPECIAL(NameSpace.SPECIAL), MEDIA(NameSpace.MEDIA);

    NameSpace namespace;

    /**
     * This will construct a PageType that specifies the NameSpace that the PageType is in.
     * @param ns The NameSpace of PageType.
     */
    PageType(NameSpace ns) {
        this.namespace = ns;
    }

    /**
     *
     * @return The NameSpace of a given PageType
     */
    public NameSpace getNamespace() {
        return namespace;
    }

    /**
     * Every PageType has a corresponding Talk-type page.
     * @return This method will let you know if a given PageType is a Talk-type page.
     */
    public boolean isTalk() {
        return (!(namespace==NameSpace.SPECIAL) && namespace.getValue()%2 == 1);
    }

    /**
     *
     * @return If the given PageType is in the Main NameSpace
     */
    public boolean isMainNs() {
        return namespace == NameSpace.MAIN;
    }

    /**
     * This is the NameSpace enum.
     * Each PageType has a specified NameSpace.
     * The main distinctions come from the three different types of
     * Main-NameSpace pages (Article, Redirect and Disambig).
     */
    public static enum NameSpace {
        MAIN(0), TALK(1),
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

        /**
         * Constructor for NameSpace.
         * @param value The numeric value of the NameSpace as defined by Wikipedia.
         */
        private NameSpace(int value) {
            this.value = (short)value;
        }

        /**
         *
         * @return The numeric value of the NameSpace as defined by Wikipedia.
         */
        public short getValue() {
            return value;
        }

        /**
         * This method lets you figure out what NameSpace is being specified when given
         * an numeric value for that NameSpace.
         * @param value The numeric value of the NameSpace as defined by Wikipedia.
         * @return The corrisponding NameSpace if it exists, or null.
         */
        static public NameSpace intToNS(int value){
            if(value>=0 && value<16) return NameSpace.values()[value];
            for (NameSpace v : NameSpace.values()){
                if (value == v.getValue()) {return v;}
            }
            return null;
        }
    }
}
