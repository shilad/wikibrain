package org.wikapidia.core.dao;

import org.jooq.DSLContext;
import org.junit.Test;
import org.wikapidia.core.dao.sql.WpDataSource;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class TestWpDataSource {
    @Test
    public void testSimple() throws IOException, ClassNotFoundException, DaoException {
        WpDataSource wpDs = TestDaoUtil.getWpDataSource();
        for (int i = 0; i < 100000; i++) {
            DSLContext context = wpDs.getJooq();
            wpDs.freeJooq(context);
        }
    }
}
