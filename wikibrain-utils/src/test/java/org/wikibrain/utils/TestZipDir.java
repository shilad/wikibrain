package org.wikibrain.utils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestZipDir {
    @Test
    public void testZipDir() throws IOException {
        File srcDir = File.createTempFile("zip-test", null);
        srcDir.delete();
        srcDir.mkdirs();
        FileUtils.write(new File(srcDir, "a"), "foo bar");
        new File(srcDir, "b").mkdir();
        FileUtils.write(new File(srcDir, "b/c"), "baz");
        FileUtils.forceDeleteOnExit(srcDir);

        File zip = File.createTempFile("zip-test", "zip");
        ZipDir.zip(srcDir, zip);
        zip.deleteOnExit();

        File destDir = File.createTempFile("zip-test", null);
        destDir.delete();
        destDir.mkdirs();
        ZipDir.unzip(zip, destDir);
        FileUtils.forceDeleteOnExit(destDir);

        File a = new File(destDir, "a");
        File c = new File(destDir, "b/c");
        assertTrue(a.isFile());
        assertTrue(c.isFile());
        assertEquals(FileUtils.readFileToString(a), "foo bar");
        assertEquals(FileUtils.readFileToString(c), "baz");
    }
}
