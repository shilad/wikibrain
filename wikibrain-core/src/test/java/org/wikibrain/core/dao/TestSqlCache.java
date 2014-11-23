package org.wikibrain.core.dao;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.junit.Test;
import org.wikibrain.core.dao.sql.MetaInfoSqlDao;
import org.wikibrain.core.dao.sql.SqlCache;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 */
public class TestSqlCache {
    @Test
    public void test() throws ClassNotFoundException, IOException, SQLException, DaoException, InterruptedException {
        File tmpDir = File.createTempFile("wikibrain-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        WpDataSource ds = TestDaoUtil.getWpDataSource();

        MetaInfoSqlDao md = new MetaInfoSqlDao(ds);
        md.beginLoad();        // create tables

        SqlCache cache = new SqlCache(md, tmpDir);

//        testCache(cache, md, "a string", "string", String.class);

        TLongIntMap map = new TLongIntHashMap(10, .5f, -1, -1);
        map.put(0, 1);
        map.put(2, 4);
        map.put(3, 4);
        testCache(cache, md, "a map", map, Boolean.class, Map.class);
    }

    private void testCache(SqlCache cache, MetaInfoDao md, String key, Object val, Class ... dependsOn) throws DaoException, InterruptedException {
        cache.put(key, val);
        assertNull(cache.get(key, dependsOn));

        Thread.currentThread().sleep(1000);

        assertNull(cache.get(key, dependsOn));
        cache.put(key, val);
        assertNull(cache.get(key, dependsOn));  // should still be null because no info in meta dao

        Thread.currentThread().sleep(1000);
        for (Class klass : dependsOn) {
            md.incrementRecords(klass);
        }

        assertNull(cache.get(key, dependsOn));
        cache.put(key, val);
        assertEquals(val, cache.get(key, dependsOn));

        Thread.currentThread().sleep(1000);
        md.incrementRecords(dependsOn[dependsOn.length - 1]);
        Thread.currentThread().sleep(1000);

        assertNull(cache.get(key, dependsOn));
    }
}
