package org.wikapidia.utils;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class WpIOUtils {
    /**
     * Deserialize an array of bytes into an object.
     * @param input Serialized stream of bytes
     * @return Deserialized object.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Serializable bytesToObject(byte input[]) throws IOException, ClassNotFoundException {
        return (Serializable) new ObjectInputStream(
                new ByteArrayInputStream(input))
                        .readObject();
    }

    /**
     * Serialize an object into bytes.
     * @param o Object to be serialized.
     * @return Serialized stream of bytes.
     * @throws IOException
     */
    public static byte[] objectToBytes(Serializable o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(out);
        oo.writeObject(o);
        oo.close();
        return out.toByteArray();
    }

    /**
     * Open a possibly compressed file and return a buffered reader for it.
     * UTF-8 encoding is used.
     * @param path
     * @return
     * @throws java.io.IOException
     */
    public static BufferedReader openReader(File path) throws IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(path));
        if (FilenameUtils.getExtension(path.toString()).toLowerCase().startsWith("bz2")) {
            input = new BZip2CompressorInputStream(input, true);
        } else if (FilenameUtils.getExtension(path.toString()).equalsIgnoreCase("gz")) {
            input = new GZIPInputStream(input);
        }
        return new BufferedReader(new InputStreamReader(input, "UTF-8"));
    }

    /**
     * Opens a buffered writer that uses UTF-8 encoding.
     * TODO: handle compression automatically based on file extension.
     * @param path
     * @return
     * @throws IOException
     */
    public static BufferedWriter openWriter(File path) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
    }
}
