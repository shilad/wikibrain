package org.wikapidia.utils;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 */
public class CompressedFile {
    /**
     * Open a possibly compressed file and return a buffered reader for it.
     * @param path
     * @return
     * @throws IOException
     */
    public static BufferedReader open(File path) throws IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(path));
        if (FilenameUtils.getExtension(path.toString()).toLowerCase().startsWith("bz2")) {
            input = new BZip2CompressorInputStream(input, true);
        } else if (FilenameUtils.getExtension(path.toString()).equalsIgnoreCase("gz")) {
            input = new GZIPInputStream(input);
        }
        return new BufferedReader(new InputStreamReader(input, "UTF-8"));
    }
}
