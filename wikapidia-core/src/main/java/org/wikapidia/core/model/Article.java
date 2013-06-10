package org.wikapidia.core.model;

import org.wikapidia.core.lang.LanguageInfo;

/**
 */
public class Article {

    private int id;
    private Title title;
    private NameSpace ns;
    private PageType type;
    private String text;
    private LanguageInfo language;

    public Article(int id, String title, NameSpace ns , PageType type, String text, LanguageInfo language)
    {
        this.id = id;
        this.title = new Title(title, language);
        this.ns = ns;
        this.type = type;
        this.text = text;
        this.language = language;
    }

    public Article(int id, Title title, NameSpace ns , PageType type, String text, LanguageInfo language)
    {
        this.id = id;
        this.title = title;
        this.ns = ns;
        this.type = type;
        this.text = text;
        this.language = language;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Title getTitle() {
        return title;
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = new Title(title, this.language);
    }

    public NameSpace getNs() {
        return ns;
    }

    public void setNs(NameSpace ns) {
        this.ns = ns;
    }

    public PageType getType() {
        return type;
    }

    public void setType(PageType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LanguageInfo getLanguage() {
        return language;
    }

    public void setLanguage(LanguageInfo language) {
        this.language = language;
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

    public static enum PageType{
        STANDARD, DISAMB, REDIRECT;

        public int getValue(){
            for (int i=0; i<3; i++){
                if (PageType.values()[i]==this){return i;}
            }
            return -1;
        }
    }

}
