package org.wikapidia.core.model;

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

    PageType(NameSpace ns) {
        this.namespace = ns;
    }

    public NameSpace getNamespace() {
        return namespace;
    }

    public boolean isTalk() {
        return (!(namespace==NameSpace.SPECIAL) && namespace.getValue()%2 == 1);
    }

    public boolean isMainNs() {
        return namespace == NameSpace.MAIN;
    }

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

        private int value;

        private NameSpace(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        static public NameSpace intToNS(int value){
            if(value>=0 && value<16) return NameSpace.values()[value];
            for (NameSpace v : NameSpace.values()){
                if (value == v.getValue()) {return v;}
            }
            return null;
        }
    }
}
