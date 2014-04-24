package org.wikibrain.core.lang;

import org.junit.Assert;
import org.junit.Test;

public class TestLanguageInfo {
    @Test
    public void testLoading() {
        Assert.assertEquals(LanguageInfo.LANGUAGE_INFOS.length, 285);
    }
}
