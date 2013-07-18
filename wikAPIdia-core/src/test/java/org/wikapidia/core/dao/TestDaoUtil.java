package org.wikapidia.core.dao;

import com.jolbox.bonecp.BoneCPDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;

/**
 */
public class TestDaoUtil {

    public static DataSource getDataSource() throws ClassNotFoundException, IOException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdir();

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }
}
