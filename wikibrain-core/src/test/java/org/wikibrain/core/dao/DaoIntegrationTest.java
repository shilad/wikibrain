package org.wikibrain.core.dao;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class DaoIntegrationTest {
    public static final File PATH_TEST_ROOT = new File("../test-root");
    public static final int LATEST_VERSION = 1;

    public void createDatabase() throws IOException {
    }

    public int getVersion() throws IOException {
        File path = new File(PATH_TEST_ROOT, "VERSION.txt");
        if (!path.isFile()) {
            return -1;
        }
        return Integer.valueOf(FileUtils.fileRead(path).trim());
    }

    public void setup() throws IOException {
        if (getVersion() != LATEST_VERSION) {
            createDatabase();
        }
    }
}
