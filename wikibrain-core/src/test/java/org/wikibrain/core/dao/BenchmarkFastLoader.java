package org.wikibrain.core.dao;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.io.FileUtils;
import org.jooq.TableField;
import org.wikibrain.core.dao.sql.FastLoader;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.jooq.Tables;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Shilad Sen
 *
 * Results on Shilad's Macbook Pro:
 * csv load, no url options: 277K records per second
 * csv load, JDB_URL_OPTS options setSchema 344K records per second
 * csv load, JDB_URL_OPTS options no setSchema 190K records per second (removed setSchema as a result of awesome batch performance)
 * non csv load, non-batch: 43K per second
 * non csv load, batch: 363K per second
 *
 */
public class BenchmarkFastLoader {
    public static int NUM_ENTRIES = 10000000;

    private static final String JDBC_URL_OPTS = ";LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0";

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.LOCAL_LINK.LANG_ID,
            Tables.LOCAL_LINK.ANCHOR_TEXT,
            Tables.LOCAL_LINK.SOURCE_ID,
            Tables.LOCAL_LINK.DEST_ID,
            Tables.LOCAL_LINK.LOCATION,
            Tables.LOCAL_LINK.IS_PARSEABLE,
            Tables.LOCAL_LINK.LOCATION_TYPE,
    };

    public static void main(String args[]) throws IOException, DaoException, SQLException {
        File dbPath = new File("tmp/benchmark-loader-db");
        if (dbPath.exists()) FileUtils.forceDelete(dbPath);
        dbPath.mkdirs();
        FileUtils.forceDeleteOnExit(dbPath);

        BoneCPDataSource ds = new BoneCPDataSource();
        System.out.println("Establishing new data source");
        ds.setJdbcUrl("jdbc:h2:" + dbPath.getAbsolutePath() + JDBC_URL_OPTS);
        ds.setUsername("sa");
        ds.setPassword("");

        ds.getConnection().createStatement().execute("DROP TABLE IF EXISTS local_link");
        String schema = "CREATE TABLE local_link (\n" +
                "  lang_id SMALLINT NOT NULL,\n" +
                "  anchor_text TEXT NOT NULL,\n" +
                "  source_id INT NOT NULL,\n" +
                "  dest_id INT NOT NULL,\n" +
                "  location INT NOT NULL,\n" +
                "  is_parseable BOOLEAN NOT NULL,\n" +
                "  location_type SMALLINT NOT NULL\n" +
                ");\n";

        ds.getConnection().createStatement().execute(schema);

        FastLoader loader = new FastLoader(new WpDataSource(ds), INSERT_FIELDS);

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < NUM_ENTRIES; i++) {
            loader.load(new Object[] {
                    10,
                    "Foo bar baz",
                    324234,
                    3219,
                    313,
                    true,
                    99
            });
        }
        long t2 = System.currentTimeMillis();
        System.err.println("insert time was " + (t2-t1) / 1000.0 + " seconds");
        System.err.println("inserted " + 1000.0 * NUM_ENTRIES / (t2-t1) + " entries per second");
        loader.endLoad();

        long t3 = System.currentTimeMillis();
        System.err.println("load time was " + (t3-t2) / 1000.0 + " seconds");
        System.err.println("loaded " + 1000.0 * NUM_ENTRIES / (t3-t2) + " entries per second");

        ResultSet rs = ds.getConnection().createStatement()
                .executeQuery("select count(*) from local_link");
        rs.next();
        System.err.println("inserted " + rs.getInt(1) + " records");
        rs = ds.getConnection().createStatement()
                .executeQuery("select * from local_link limit 100000");

        while (rs.next()) {
            assert(rs.getInt(1) == 10);
            assert(rs.getString(2).equals("Foo bar baz"));
            assert(rs.getInt(3) == 324234);
            assert(rs.getInt(4) == 3219);
            assert(rs.getInt(5) == 313);
            assert(rs.getBoolean(6));
            assert(rs.getInt(7) == 99);
        }
    }
}
