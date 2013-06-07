package org.wikapidia.core.model;

public enum PageType {

    ARTICLE(0), REDIRECT(0), DISAMBIG(0), TALK(1),
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

    int namespace;

    PageType(int ns) {
        this.namespace = ns;
    }

    public int getNamespace() {
        return namespace;
    }

    public int isTalk() {
        throw new UnsupportedOperationException();
    }

    public boolean isMainNs() {
        return namespace == 0;
    }
}
