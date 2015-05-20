package org.wikibrain.core.dao.sql;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.io.FileUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.sql.WpDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 */
public class TestDaoUtil {

    public static DataSource getDataSource() throws ClassNotFoundException, IOException, DaoException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikibrain-h2", null);
        tmpDir.delete();
        tmpDir.mkdirs();

        DataSource ds = getDataSource(tmpDir);

        FileUtils.forceDeleteOnExit(tmpDir);
        return ds;
    }

    public static WpDataSource getWpDataSource() throws IOException, ClassNotFoundException, DaoException {
        return new WpDataSource(getDataSource());
    }

    public static WpDataSource getWpDataSource(File file) throws IOException, ClassNotFoundException, DaoException {
        return new WpDataSource(getDataSource(file));
    }

    public static DataSource getDataSource(File file) throws ClassNotFoundException, IOException, DaoException {
        Class.forName("org.h2.Driver");

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setPartitionCount(8);
        ds.setMaxConnectionsPerPartition(4);
        ds.setJdbcUrl("jdbc:h2:"+file.getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");

        // Initialize the database to create files
        try {
            ds.getConnection().close();
        } catch (SQLException e) {
            throw new DaoException(e);
        }

        return ds;
    }
}
