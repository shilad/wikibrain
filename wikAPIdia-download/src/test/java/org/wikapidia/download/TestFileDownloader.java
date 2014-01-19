package org.wikapidia.download;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TODO: This is really an integration test, but because it runs so quickly I've left it as a unit test.
 * @author Shilad Sen
 */
public class TestFileDownloader {
    @Test
    public void testDownloader() throws IOException, InterruptedException {
        URL url = new URL("http://www.google.com/robots.txt");
        File tmp1 = File.createTempFile("downloader-test", ".txt");
        FileDownloader downloader = new FileDownloader();
        downloader.download(url, tmp1);
        assertTrue(tmp1.isFile());
        List<String> lines = FileUtils.readLines(tmp1);
        assert(lines.size() > 10);
        assertTrue(lines.get(0).startsWith("User-agent:"));

        File tmp2 = File.createTempFile("downloader-test", ".txt");
        FileUtils.copyURLToFile(url, tmp2);
        assertTrue(tmp2.isFile());
        assertTrue(FileUtils.readFileToString(tmp2).startsWith("User-agent:"));
        assertEquals(FileUtils.readFileToString(tmp1), FileUtils.readFileToString(tmp2));
    }
}
