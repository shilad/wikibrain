package org.wikibrain.utils;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class SevenZipBuffer implements Closeable {
    private static final int BUFFER_SIZE = 1024*1024;

    private final File file;
    private final SevenZFile stream;
    private final byte [] buffer = new byte[BUFFER_SIZE];
    private TByteArrayList bytes = new TByteArrayList();
    private long fileLength;
    private long totalBytes = 0;

    public SevenZipBuffer(File file) throws IOException {
        this.file = file;
        this.fileLength = file.length();
        this.stream = new SevenZFile(file);
        this.stream.getNextEntry();
    }

    public String readUntil(String ... markers) throws IOException {
        byte [][] markerBytes = new byte[markers.length][];
        for (int i = 0; i < markers.length; i++) {
            markerBytes[i] = markers[i].getBytes();
        }

        int lastSize = 0;
        while (true) {
            int minI = Integer.MAX_VALUE;
            byte [] minMarker = null;

            for (byte[] marker : markerBytes) {
                int i = indexOf(bytes, Math.max(0, lastSize - marker.length), marker);
                if (i >= 0 && i < minI) {
                    minI = i;
                    minMarker = marker;
                }
            }

            if (minMarker != null)  {
                String match = new String(bytes.toArray(), 0, minI + minMarker.length, "UTF-8");
                bytes.remove(0, minI + minMarker.length);
                return match;
            }

            int n = stream.read(buffer);
            if (n < 0) {
                return null;
            }
            totalBytes += n;
            lastSize = bytes.size();
            bytes.add(buffer, 0, n);
        }

    }

    public int indexOf(TByteList bytes, int minOffset, byte [] query) {
        for (int i = minOffset; i < bytes.size() - query.length; i++) {
            if (bytesAre(bytes, i, query)) {
                return i;
            }
        }
        return -1;
    }

    public boolean bytesAre(TByteList bytes, int offset, byte[] query) {
        if (offset + query.length > bytes.size()) {
            return false;
        }
        for (int i = 0; i < query.length; i++) {
            if (bytes.get(offset + i) != query[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public double getPercentCompleted() {
        return 100.0 * totalBytes / fileLength;
    }
}
