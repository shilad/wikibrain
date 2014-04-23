package org.wikibrain.core.dao;

import org.junit.Test;
import org.wikibrain.core.dao.sql.InterLanguageLinkSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestInterLanguageLinkSqlDao {

    @Test
    public void testIlls() throws DaoException, IOException, ClassNotFoundException {
        WpDataSource ds = TestDaoUtil.getWpDataSource();
        InterLanguageLinkSqlDao dao = new InterLanguageLinkSqlDao(ds);
        dao.beginLoad();
        dao.save(new InterLanguageLink(Language.EN, 3, Language.DE, 5));
        dao.save(new InterLanguageLink(Language.EN, 3, Language.DE, 9));
        dao.save(new InterLanguageLink(Language.EN, 3, Language.SIMPLE, 9));
        dao.save(new InterLanguageLink(Language.EN, 5, Language.SIMPLE, 9));
        dao.save(new InterLanguageLink(Language.DE, 3, Language.SIMPLE, 9));
        dao.endLoad();

        Set<LocalId> l1 = dao.getFromSource(Language.EN, 3);
        assertTrue(l1.contains(new LocalId(Language.DE, 5)));
        assertTrue(l1.contains(new LocalId(Language.DE, 9)));
        assertTrue(l1.contains(new LocalId(Language.SIMPLE, 9)));
        assertEquals(3, l1.size());

        Set<LocalId> l2 = dao.getFromSource(Language.DE, 5);
        assertEquals(0, l2.size());

        Set<LocalId> l3 = dao.getFromSource(Language.DE, 3);
        assertEquals(1, l3.size());
        assertTrue(l3.contains(new LocalId(Language.SIMPLE, 9)));

        Set<LocalId> l4 = dao.getToDest(Language.DE, 3);
        assertEquals(0, l4.size());

        Set<LocalId> l5 = dao.getToDest(Language.SIMPLE, 9);
        assertEquals(3, l5.size());
        assertTrue(l5.contains(new LocalId(Language.DE, 3)));
        assertTrue(l5.contains(new LocalId(Language.EN, 3)));
        assertTrue(l5.contains(new LocalId(Language.EN, 5)));
    }
}
