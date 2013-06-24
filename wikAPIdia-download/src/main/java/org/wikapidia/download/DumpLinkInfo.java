package org.wikapidia.download;

import org.apache.commons.io.IOUtils;
import org.wikapidia.core.lang.Language;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class DumpLinkInfo {

    private static final Logger LOG = Logger.getLogger(DumpLinkGetter.class.getName());

    private final Language language;
    private final String date;
    private final LinkMatcher linkMatcher;
    private final URL url;

    public DumpLinkInfo(Language language, String date, LinkMatcher linkMatcher, URL url) {
        this.language = language;
        this.date = date;
        this.linkMatcher = linkMatcher;
        this.url = url;
    }

    public DumpLinkInfo(String langCode, String date, String linkMatcher, String url) throws MalformedURLException {
        this.language = Language.getByLangCode(langCode);
        this.date = date;
        this.linkMatcher = LinkMatcher.getByName(linkMatcher);
        this.url = new URL(url);
    }

    public Language getLanguage() {
        return language;
    }

    public String getDate() {
        return date;
    }

    public LinkMatcher getLinkMatcher() {
        return linkMatcher;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * Parses a file of info pertaining to dump links into a list of DumpLinkInfo.
     * Info must be listed in order: lang code, date, LinkMatcher, URL
     * with each DumpLink reference on a new line.
     * @param file
     * @return
     */
    public static List<DumpLinkInfo> parseFile(String file) {
        InputStream stream = null;
        try {
            stream = DumpLinkInfo.class.getClassLoader()
                    .getResourceAsStream(file);
            List<String> lines = IOUtils.readLines(stream, "UTF-8");
            List<DumpLinkInfo> dumpLinks = new ArrayList<DumpLinkInfo>();
            for (String line : lines) {
                DumpLinkInfo temp = parseInfo(line);
                if (temp != null) {
                    dumpLinks.add(temp);
                }
            }
            return dumpLinks;
        } catch (IOException e) {
            throw new RuntimeException(e);  // What else can we do?
        } finally {
            if (stream != null) IOUtils.closeQuietly(stream);
        }
    }

    /**
     * Parses a tab-separated string of info into a DumpLink.
     * Info must be listed in order: lang code, date, LinkMatcher, URL
     * @param info
     * @return
     * @throws MalformedURLException
     */
    public static DumpLinkInfo parseInfo(String info) {
        String[] parsedInfo = info.split("\t");

        try {
            return new DumpLinkInfo(
                    parsedInfo[0],
                    parsedInfo[1],
                    parsedInfo[2],
                    parsedInfo[3]
            );
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Malformed URL \"" + parsedInfo[3] + "\" : ", e);
        }
        return null;

//        Language language = null;
//        int date = -1;
//        LinkMatcher linkMatcher = null;
//        URL url = null;
//        for (String infoPiece : parsedInfo) {
//            LinkMatcher temp = LinkMatcher.getByName(infoPiece);
//            if (temp != null) {
//                linkMatcher = temp;
//            }
//            try {
//                language = Language.getByLangCode(infoPiece);
//            } catch (IllegalArgumentException e) {}
//            try {
//                date = Integer.parseInt(infoPiece);
//            } catch (NumberFormatException e) {}
//            try {
//                url = new URL(infoPiece);
//            } catch (MalformedURLException e) {}
//        }
//        return new DumpLinkInfo(
//                language,
//                date,
//                linkMatcher,
//                url
//        );
    }
}
