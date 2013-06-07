package org.wikapidia.core.model;

import org.junit.Test;
import org.wikapidia.core.lang.Language;

import static org.junit.Assert.*;

public class TestLanguage {

    @Test
    public void testByLangCode() {
        Language en = Language.getByLangCode("en");
        assertEquals(en.getId(), 1);
        assertEquals(en.getLangCode(), "en");
        assertEquals(en.getEnLangName(), "English");
        assertEquals(en.getNativeName(), "English");

        Language tr = Language.getByLangCode("tr");
        assertEquals(tr.getId(), 23);
        assertEquals(tr.getLangCode(), "tr");
        assertEquals(tr.getEnLangName(), "Turkish");
        assertEquals(tr.getNativeName(), "Türkçe");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNonexistentByLangCode() {
        Language.getByLangCode("zz");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNonexistentById() {
        Language.getById(-1);
    }

    @Test
    public void testById() {
        Language en = Language.getById(1);
        assertEquals(en.getId(), 1);
        assertEquals(en.getLangCode(), "en");
        assertEquals(en.getEnLangName(), "English");
        assertEquals(en.getNativeName(), "English");

        Language tr = Language.getById(23);
        assertEquals(tr.getId(), 23);
        assertEquals(tr.getLangCode(), "tr");
        assertEquals(tr.getEnLangName(), "Turkish");
        assertEquals(tr.getNativeName(), "Türkçe");
    }
}
