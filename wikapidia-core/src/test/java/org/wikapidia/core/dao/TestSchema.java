package org.wikapidia.core.dao;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;

/**
 * Makes sure a schema can be created
 */
public class TestSchema {
    @Test
    public void testSchema() throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();
        try {
            Connection conn = DriverManager.
                    getConnection("jdbc:h2:" + new File(tmpDir, "db").getAbsolutePath());
            conn.createStatement().execute(
                    FileUtils.readFileToString(new File("src/main/resources/schema.sql"))
            );
            conn.close();
        } finally {
            FileUtils.deleteDirectory(tmpDir);
        }
    }
}
