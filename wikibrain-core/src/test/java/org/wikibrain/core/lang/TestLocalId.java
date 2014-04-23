package org.wikibrain.core.lang;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestLocalId {

    @Test
    public void testPack() {
        testPack(1, 0);
        testPack(1, 1);
        testPack(1, (1<<26) - 1);
        testPack(31, (1<<25) - 1);
        testPack(31, (1<<26) - 1);
        testPack(63, (1<<26) - 1);
        testPack(63, (1<<25) - 1);
        testPack(63, (1<<25) - 2);
        testPack(63, 1);
        testPack(63, 0);
    }

    private void testPack(int langId, int id) {
        LocalId lid = new LocalId(Language.getById(langId), id);
        int packed = lid.toInt();
        LocalId lid2 = LocalId.fromInt(packed);
        assertEquals(lid, lid2);
        long lpacked = lid.toLong();
        LocalId lid3 = LocalId.fromLong(lpacked);
        assertEquals(lid, lid3);
    }

}
