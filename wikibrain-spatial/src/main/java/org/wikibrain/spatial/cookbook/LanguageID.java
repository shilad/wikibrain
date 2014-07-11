package org.wikibrain.spatial.cookbook;

import org.wikibrain.core.lang.Language;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by toby on 7/1/14.
 */
public class LanguageID {
    public static void main(String[] args) throws Exception {

        List<Language> langList = new LinkedList<Language>();

        langList.add(Language.getByLangCode("en"));

        langList.add(Language.getByLangCode("ar"));
        langList.add(Language.getByLangCode("ca"));
        //langList.add(Language.getByLangCode("ceb"));
        langList.add(Language.getByLangCode("cs"));
        langList.add(Language.getByLangCode("de"));
        langList.add(Language.getByLangCode("es"));
        //langList.add(Language.getByLangCode("fa"));
        langList.add(Language.getByLangCode("fi"));
        langList.add(Language.getByLangCode("fr"));
        langList.add(Language.getByLangCode("hu"));
        //langList.add(Language.getByLangCode("id"));
        langList.add(Language.getByLangCode("it"));
        langList.add(Language.getByLangCode("ja"));
        langList.add(Language.getByLangCode("ko"));
        langList.add(Language.getByLangCode("nl"));
        langList.add(Language.getByLangCode("no"));
        //langList.add(Language.getByLangCode("pl"));
        langList.add(Language.getByLangCode("pt"));
        langList.add(Language.getByLangCode("ru"));
        langList.add(Language.getByLangCode("sv"));
        //langList.add(Language.getByLangCode("uk"));
        langList.add(Language.getByLangCode("vi"));
        //langList.add(Language.getByLangCode("war"));
        langList.add(Language.getByLangCode("zh"));
        for(Language language : langList){
            System.out.println(language.getEnLangName() + " " + language.getId());
        }
    }
}
