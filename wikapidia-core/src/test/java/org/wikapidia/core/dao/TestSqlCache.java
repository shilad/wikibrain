package org.wikapidia.core.dao;

import com.jolbox.bonecp.BoneCPDataSource;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.junit.Test;
import org.wikapidia.core.dao.sql.SqlCache;
import org.wikapidia.core.lang.LanguageInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 */
public class TestSqlCache {
    @Test
    public void test() throws ClassNotFoundException, IOException, SQLException, DaoException{
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");

        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        SqlCache cache = new SqlCache(ds, tmpDir);
        cache.makeLastModifiedDb();
        TLongIntMap map = new TLongIntHashMap(10, .5f, -1, -1);
        map.put(0, 1);
        map.put(2, 4);
        map.put(3, 4);
        String string = "string";
        cache.saveToCache("A string", string);
        cache.saveToCache("A map", map);

        assert (cache.get("A string", "nothing!")==null);

        cache.updateTableLastModified("table");
        assert (cache.get("A string", "table")==null);

        cache.saveToCache("A string", string);
        System.out.print(cache.get("A string", "table"));
        assert (cache.get("A string", "table").equals(string));


        cache.updateTableLastModified("table");
        assert (cache.get("A map", "table")==null);

        cache.saveToCache("A map", map);
        assert (cache.get("A map", "table").equals(map));
    }
}
