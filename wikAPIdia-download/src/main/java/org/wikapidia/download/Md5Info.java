package org.wikapidia.download;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ari Weiland
 *
 */
public class Md5Info {
    private final Map<String, String> md5Strings = new HashMap<String, String>();
    private final File md5File;

    /**
     * Constructs a new MD5 dump file from the specified file.
     * If file is not an MD5 dump file, there will be blood...
     * @param md5File
     */
    public Md5Info(File md5File) {
        this.md5File = md5File;
        read();
    }

    public File getFile() {
        return md5File;
    }

    /**
     * returns the MD5 Hex string for the specified dump link
     * @param linkInfo
     * @return
     */
    public String getMd5String(DumpLinkInfo linkInfo) {
        return md5Strings.get(linkInfo.getDownloadName());
    }

    private void read() {
        InputStream stream = null;
        try {
            stream = FileUtils.openInputStream(md5File);
            List<String> lines = IOUtils.readLines(stream, "UTF-8");
            for (String line : lines) {
                String[] md5s = line.split(" ");
                md5Strings.put(md5s[1], md5s[0]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);  // What else can we do?
        } finally {
            if (stream != null) IOUtils.closeQuietly(stream);
        }
    }
}
