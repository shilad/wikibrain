package org.wikibrain.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestJvmUtils {
    @Test
    public void testFullClassName() {
        assertEquals("org.wikibrain.utils.JvmUtils", JvmUtils.getFullClassName("JvmUtils"));
        assertNull(JvmUtils.getFullClassName("Foozkjasdf"));
    }

    @Test
    public void testClassForShortName() {
        assertEquals(JvmUtils.class, JvmUtils.classForShortName("JvmUtils"));
        assertNull(JvmUtils.classForShortName("Foozkjasdf"));
    }
}
