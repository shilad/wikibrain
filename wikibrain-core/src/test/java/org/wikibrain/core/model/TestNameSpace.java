package org.wikibrain.core.model;

import org.junit.Test;

public class TestNameSpace {
    @Test
    public void test(){
        assert (NameSpace.getNameSpaceByName("HELP").equals(NameSpace.HELP));
        assert (NameSpace.getNameSpaceByName("Help").equals(NameSpace.HELP));
        assert (NameSpace.getNameSpaceByName("HELP TALK").equals(NameSpace.HELP_TALK));
        assert (NameSpace.getNameSpaceByName("WP").equals(NameSpace.WIKIPEDIA));
    }
}
