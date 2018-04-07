package org.wikibrain.sr.word2vec;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

public class Word2VecReader {
    private static final Logger LOG = LoggerFactory.getLogger(Word2VecReader.class);

    public static class WordAndVector {
        private String word;
        private float[] vector;

        public WordAndVector(String word, float[] vector) {
            this.word = word;
            this.vector = vector;
        }

        public String getWord() {
            return word;
        }

        public float[] getVector() {
            return vector;
        }

        public boolean isArticle() {
            return (word.startsWith("/w/"));
        }

        public int getArticleId() {
            if (word.startsWith("/w/")) {
                String[] pieces = word.split("/", 5);
                return Integer.valueOf(pieces[3]);
            } else {
                return -1;
            }
        }
    }

    /**
     * Reads a binary Mikolov-style word2vec file.
     * @throws IOException
     */
    public static WordAndVectorIterator readBinary(String path) throws IOException {
        return new BinaryIterator(path);
    }

    /**
     * Reads a text Mikolov-style word2vec file.
     * @throws IOException
     */
    public static WordAndVectorIterator readText(String path) throws IOException {
        return new TextIterator(path);
    }


    /**
     * Reads a binary or text Mikolov-style file. Tries to guess which format it is.
     * @throws IOException
     */
    public static WordAndVectorIterator read(String path) throws IOException {
        return read(new File(path));
    }

    /**
     * Reads a binary or text Mikolov-style file. Tries to guess which format it is.
     * @throws IOException
     */
    public static WordAndVectorIterator read(File path) throws IOException {

        InputStream bis = WpIOUtils.openInputStream(path);
        DataInputStream dis = new DataInputStream(bis);

        while (true) {
            char c = (char) dis.read();
            if (c == '\n') break;
        }

        // Read word (hopefully, but maybe not because of UTF-8)
        while (true) {
            char c = (char) dis.read();
            if (c == ' ') break;
        }

        // Read numeric value. Text format should be all digits up to next space.
        boolean isDigits = true;
        while (isDigits) {
            char c = (char) dis.read();
            if (c == ' ') break;
            isDigits = Character.isDigit(c) || c == '.' || c == '-';
        }

        IOUtils.closeQuietly(bis);
        IOUtils.closeQuietly(dis);

        if (isDigits) {
            return new TextIterator(path.getPath());
        } else {
            return new BinaryIterator(path.getPath());
        }
    }

    static String readString(DataInputStream dis) throws IOException {
        TByteList bytes = new TByteArrayList();
        while (true) {
            int i = dis.read();
            if (i == -1) {
                break;
            }
            if (i < 0 || i > 255) {
                throw new IllegalStateException();
            }
            char c = (char)i;
            if (c == ' ') {
                break;
            }
            if (c != '\n') {
                bytes.add((byte)i);
            }
        }
        return new String(bytes.toArray(), "UTF-8");
    }

    static float readFloat(InputStream is) throws IOException {
        byte[] bytes = new byte[4];
        is.read(bytes);
        return getFloat(bytes);
    }

    private static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum | (b[0] & 0xff) << 0;
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }

    public interface WordAndVectorIterator extends Iterator<WordAndVector> {
        public int getVectorLength();
        public int getNumVectors();
    }

    static class BinaryIterator implements WordAndVectorIterator {
        private WordAndVector buffer = null;
        private DataInputStream dis = null;
        private InputStream bis = null;
        private int numEntities;
        private int index = 0;
        private int vlength;

        BinaryIterator(String path) throws IOException {
            bis = WpIOUtils.openInputStream(new File(path));
            dis = new DataInputStream(bis);

            String header = "";
            while (true) {
                char c = (char) dis.read();
                if (c == '\n') break;
                header += c;
            }

            String tokens[] = header.split(" ");

            numEntities = Integer.parseInt(tokens[0]);
            vlength = Integer.parseInt(tokens[1]);
            LOG.info("preparing to read " + numEntities + " with length " + vlength + " vectors");
        }

        @Override
        public boolean hasNext() {
            advance();
            return (buffer != null);
        }

        @Override
        public WordAndVector next() {
            advance();
            WordAndVector res = buffer;
            buffer = null;
            return res;
        }

        private void advance() {
            while (buffer == null && index < numEntities) {
                index++;
                try {
                    String word = readString(dis);
                    if (index % 5000 == 0) {
                        LOG.info("Read word vector " + word + " (" + index + " of " + numEntities + ")");
                    }

                    float[] vector = new float[vlength];
                    double norm2 = 0.0;
                    for (int j = 0; j < vlength; j++) {
                        float val = readFloat(dis);
                        norm2 += val * val;
                        vector[j] = val;
                    }
                    norm2 = Math.sqrt(norm2);

                    for (int j = 0; j < vlength; j++) {
                        vector[j] /= norm2;
                    }
                    buffer = new WordAndVector(word, vector);
                } catch (IOException e) {
                    LOG.error("Error occured while loading vector", e);
                }
            }
            if (index >= numEntities) {
                IOUtils.closeQuietly(bis);
                IOUtils.closeQuietly(dis);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getVectorLength() {
            return vlength;
        }

        @Override
        public int getNumVectors() {
            return numEntities;
        }
    }

    static class TextIterator implements WordAndVectorIterator {
        private WordAndVector buffer = null;
        private BufferedReader in = null;
        private int numEntities;
        private int index = 0;
        private int vlength;

        TextIterator(String path) throws IOException {
            in = WpIOUtils.openBufferedReader(new File(path));

            String tokens[] = in.readLine().trim().split(" ");

            numEntities = Integer.parseInt(tokens[0]);
            vlength = Integer.parseInt(tokens[1]);
            LOG.info("preparing to read " + numEntities + " with length " + vlength + " vectors");        }

        @Override
        public boolean hasNext() {
            advance();
            return (buffer != null);
        }

        @Override
        public WordAndVector next() {
            advance();
            WordAndVector res = buffer;
            buffer = null;
            return res;
        }

        private void advance() {
            while (buffer == null && index < numEntities) {
                index++;
                try {
                    String line = in.readLine();
                    if (line.endsWith("\n")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    String [] tokens = line.split(" ");
                    String word = tokens[0];
                    if (index % 5000 == 0) {
                        LOG.info("Read word vector " + word + " (" + index + " of " + numEntities + ")");
                    }
                    if (tokens.length != vlength + 1) {
                        LOG.warn("Expected vector of length " + (vlength + 1) + " but found " + tokens.length + " in " + line);
                        continue;
                    }

                    float[] vector = new float[vlength];
                    double norm2 = 0.0;
                    for (int j = 0; j < vlength; j++) {
                        float val = Float.valueOf(tokens[j+1]);
                        norm2 += val * val;
                        vector[j] = val;
                    }
                    norm2 = Math.sqrt(norm2);

                    for (int j = 0; j < vlength; j++) {
                        vector[j] /= norm2;
                    }
                    buffer = new WordAndVector(word, vector);
                } catch (IOException e) {
                    LOG.error("Error occured while loading vector", e);
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getVectorLength() {
            return vlength;
        }

        @Override
        public int getNumVectors() {
            return numEntities;
        }
    }
}
