package org.wikibrain.download;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.lang.Language;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * A wrapper class for storing and processing information about a dump link.
 * Contains static parser methods to generate DumpLinkInfo instances, and
 * useful getters for all parameters plus custom information related to
 * processing and downloading a dump.
 *
 * @author Ari Weiland
 *
 */
public class DumpLinkInfo {

    private static final Logger LOG = LoggerFactory.getLogger(DumpLinkGetter.class);

    private Language language;
    private String date;
    private FileMatcher linkMatcher;
    private URL url;
    private String md5;
    private int counter;

    public DumpLinkInfo(Language language, String date, FileMatcher linkMatcher, URL url, int counter) {
        this.language = language;
        this.date = date;
        this.linkMatcher = linkMatcher;
        this.url = url;
        this.counter = counter;
    }

    public DumpLinkInfo(String langCode, String date, String linkMatcher, String url, String md5, int counter) throws MalformedURLException {
        this.language = Language.getByLangCode(langCode);
        this.date = date;
        this.linkMatcher = FileMatcher.getByName(linkMatcher);
        this.url = new URL(url);
        this.md5 = md5;
        this.counter = counter;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public FileMatcher getLinkMatcher() {
        return linkMatcher;
    }

    public void setLinkMatcher(FileMatcher linkMatcher) {
        this.linkMatcher = linkMatcher;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    /**
     * Returns a string for the local path in which to save this dump file
     * @return
     */
    public String getLocalPath() {
        return language.getLangCode() + "/" + date;
    }

    /**
     * Returns a string for the file name with which to save this dump file
     * @return
     */
    public String getFileName() {
        return FilenameUtils.getName(url.getPath());
    }

    public String getDownloadName() {
        return url.toString().substring(url.toString().lastIndexOf("/") + 1);
    }

    /**
     * Parses a file of info pertaining to dump links into a cluster of DumpLinkInfo.
     * Info must be listed in order: lang code, date, FileMatcher, URL, MD5 checksum
     * with each DumpLink reference on a new line.
     * @param file
     * @return
     */
    public static DumpLinkCluster parseFile(File file) {
        InputStream stream = null;
        try {
            stream = FileUtils.openInputStream(file);
            List<String> lines = IOUtils.readLines(stream, "UTF-8");
            DumpLinkCluster dumpLinks = new DumpLinkCluster();
            for (String line : lines) {
                String[] parsedInfo = line.split("\t");
                String langCode     = parsedInfo[0];
                String date         = parsedInfo[1];
                String linkMatcher  = parsedInfo[2];
                String counter      = parsedInfo[3];
                String url          = parsedInfo[4];
                String md5 = null;
                if (parsedInfo.length == 6) md5 = parsedInfo[5];
                try {
                    DumpLinkInfo temp = new DumpLinkInfo(
                            langCode,
                            date,
                            linkMatcher,
                            url,
                            md5,
                            Integer.valueOf(counter)
                    );
                    dumpLinks.add(temp);
                } catch (MalformedURLException e) {
                    LOG.warn("Malformed URL \"" + url + "\" : ", e);
                }
            }
            return dumpLinks;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Something went horribly wrong!
        } finally {
            if (stream != null) IOUtils.closeQuietly(stream);
        }
    }
}
