package org.wikapidia.core.model;

import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;

/**
 * Created with IntelliJ IDEA.
 * User: logger
 * Date: 6/17/13
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestTitle {
    @Test
    public void TestTitle(){
        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        Title pokemon = new Title("Pokemon: The Movie",lang);
        Title axelson = new Title("Ax:son Johnson family",lang);
        assert(pokemon.getNamespaceString() == null);
        assert(axelson.getNamespaceString() == null);
    }
}
