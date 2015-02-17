package org.wikibrain.download;

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

    @Test
    public void testDownloaderMove() throws IOException, InterruptedException {
        URL url = new URL("http://dumps.wikimedia.org/iewiki/20150123/iewiki-20150123-redirect.sql.gz");
        File tmp1 = File.createTempFile("downloader-test", ".txt");
        File tmp3 = File.createTempFile("downloader-test", ".txt");
        tmp1.delete();
        tmp3.delete();
        tmp3.deleteOnExit();
        FileDownloader downloader = new FileDownloader();
        downloader.download(url, tmp3);
        assertTrue(tmp3.isFile());
        FileUtils.moveFile(tmp3, tmp1);
    }
}
