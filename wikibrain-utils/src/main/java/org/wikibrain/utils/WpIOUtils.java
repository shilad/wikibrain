package org.wikibrain.utils;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class WpIOUtils {

    public static void mkdirsQuietly(File dir) {
        if (!dir.isDirectory()) {
            FileUtils.deleteQuietly(dir);
            dir.mkdirs();
        }
    }

    public static void writeObjectToFile(File file, Object o) throws IOException {
        ObjectOutputStream oop = new ObjectOutputStream(new FileOutputStream(file));
        oop.writeObject(o);
        oop.close();
    }

    public static Object readObjectFromFile(File file) throws IOException {
        ObjectInputStream oip = null;
        try {
            oip = new ObjectInputStream(new FileInputStream(file));
            return oip.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        } finally {
            if (oip != null) IOUtils.closeQuietly(oip);
        }
    }

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
     * Return the most recent tstamp among files in a diretory.
     * @param dir
     * @return
     */
    public static long getLastModifiedfromDir(File dir){
        long latest = -1;
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                if (f.lastModified() > latest) {
                    latest = f.lastModified();
                }
            }
        }
        return latest;
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
    public static BufferedReader openBufferedReader(File path) throws IOException {
        return new BufferedReader(openReader(path));
    }

    /**
     * Opens a resource in the classpath as a buffered reader.
     * @param path
     * @return
     * @throws IOException
     */
    public static BufferedReader openResource(String path) throws IOException {
        InputStream is = WpIOUtils.class.getResourceAsStream(path);
        return new BufferedReader(new InputStreamReader(is, "utf-8"));
    }

    /**
     * Reads a resource on the classpath into a string and returns it.
     * @param path
     * @return
     * @throws IOException
     */
    public static String resourceToString(String path) throws IOException {
        InputStream is = WpIOUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("Unknown resource: " + path);
        }
        try {
            return IOUtils.toString(is, "utf-8");
        } finally {
            if (is != null) is.close();
        }
    }

    /**
     * Open a possibly compressed file and return a reader for it.
     * UTF-8 encoding is used.
     * @param path
     * @return
     * @throws java.io.IOException
     */
    public static Reader openReader(File path) throws IOException {
        InputStream input = openInputStream(path);
        return new InputStreamReader(input, "UTF-8");
    }

    /**
     * Opens a possibly compressed input stream.
     * The underlying input stream is, in fact buffered even though
     * the returned object isn't.
     * @param path
     * @return
     * @throws IOException
     */
    public static InputStream openInputStream(File path) throws IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(path));
        if (FilenameUtils.getExtension(path.toString()).toLowerCase().startsWith("bz2")) {
            input = new BZip2CompressorInputStream(input, true);
        } else if (FilenameUtils.getExtension(path.toString()).equalsIgnoreCase("gz")) {
            input = new GZIPInputStream(input);
        }
        return input;
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

    /**
     * Opens a buffered writer that uses UTF-8 encoding.
     * TODO: handle compression automatically based on file extension.
     * @param path
     * @return
     * @throws IOException
     */
    public static BufferedWriter openWriterForAppend(File path) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true), "UTF-8"));
    }

    public static BufferedWriter openBZ2Writer(File path) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(
                new BZip2CompressorOutputStream(new FileOutputStream(path)),
               "UTF-8"));
    }

    /**
     * Creates a new temporary directory
     * @param name Name to be embedded within the tmp dir
     * @param deleteOnExit If true, try to delete the directory when the JVM exits.
     * @return
     * @throws IOException
     */
    public static File createTempDirectory(String name, boolean deleteOnExit) throws IOException {
        File tmpDir = File.createTempFile(name, null);
        if(!tmpDir.delete()) {
            throw new IOException("Could not delete temp file: " + tmpDir.getAbsolutePath());
        }
        if(!tmpDir.mkdir()) {
            throw new IOException("Could not create temp directory: " + tmpDir.getAbsolutePath());
        }
        if (deleteOnExit) {
            FileUtils.forceDeleteOnExit(tmpDir);
        }
        return tmpDir;
    }

    /**
     * @see WpIOUtils#createTempDirectory(String, boolean)
     * @return
     * @throws IOException
     */
    public static File createTempDirectory(String name) throws IOException {
        return createTempDirectory(name, true);
    }


    /**
     * Gets the path relative to a specified directory.
     * @param base
     * @param path
     * @return
     */
    public static String getRelativePath(File base, File path) {
        String cleanedBase = FilenameUtils.normalize(base.toString());
        String cleanedPath = FilenameUtils.normalize(path.toString());
        return new File(cleanedBase).toURI().relativize(new File(cleanedPath).toURI()).getPath();
    }

    public static BufferedWriter openWriter(String path) throws IOException {
        return openWriter(new File(path));
    }
}
