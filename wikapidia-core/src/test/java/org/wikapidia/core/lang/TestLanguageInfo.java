package org.wikapidia.core.lang;

import org.junit.Assert;
import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;

import static org.junit.Assert.*;

public class TestLanguageInfo {
    @Test
    public void testLoading() {
        Assert.assertEquals(LanguageInfo.LANGUAGE_INFOS.length, 285);
    }
}
