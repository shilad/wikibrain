package org.wikapidia.core.model;

import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;


public class TestTitle {
    @Test
    public void TestTitle(){
        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        Title pokemon = new Title("Pokemon: The Movie",lang);
        assert (pokemon.getNamespaceString() == null);
        assert (pokemon.getNamespace()==NameSpace.ARTICLE);
        assert (pokemon.getTitleStringWithoutNamespace().equals("Pokemon: The Movie"));
        Title axelson = new Title("Ax:son Johnson family",lang);
        assert (axelson.getNamespaceString() == null);
        assert (axelson.getNamespace()==NameSpace.ARTICLE);
        assert(axelson.getTitleStringWithoutNamespace().equals("Ax:son Johnson family"));
        Title pokemonTalk = new Title("Talk:Pokemon: The Movie",lang);
        assert (pokemonTalk.getNamespaceString().equals("Talk"));
        assert (pokemonTalk.getNamespace()==NameSpace.TALK);
        assert (pokemonTalk.getTitleStringWithoutNamespace().equals("Pokemon: The Movie"));

    }
}
