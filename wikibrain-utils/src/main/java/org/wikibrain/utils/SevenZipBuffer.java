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
    private static final int BUFFER_SIZE = 64 * 1024;

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

    public boolean skipUntil(String ... markers) throws IOException {
        int maxMarkerBytes = -1;
        byte [][] markerBytes = new byte[markers.length][];
        for (int i = 0; i < markers.length; i++) {
            markerBytes[i] = markers[i].getBytes();
            maxMarkerBytes = Math.max(maxMarkerBytes, markerBytes[i].length);
        }

        // Prime the beginning of the buffer, if possible.
        int bufferEnd = Math.min(maxMarkerBytes, bytes.size());
        System.arraycopy(bytes.toArray(), bytes.size() - bufferEnd, buffer, 0, bufferEnd);

        while (true) {
            int minI = Integer.MAX_VALUE;
            byte [] minMarker = null;

            for (byte[] marker : markerBytes) {
                int i = indexOf(buffer, 0, bufferEnd, marker);
                if (i >= 0 && i < minI) {
                    minI = i;
                    minMarker = marker;
                }
            }

            if (minMarker != null)  {
                bytes.resetQuick();
                bytes.add(buffer, minI + minMarker.length, bufferEnd);
                return true;
            }

            int n = stream.read(buffer, bufferEnd, buffer.length - bufferEnd);
            if (n < 0) {
                return false;
            }
            totalBytes += n;
            bufferEnd += n;

            int numRetain = Math.min(maxMarkerBytes, bufferEnd);
            System.arraycopy(buffer, bufferEnd - numRetain, buffer, 0, numRetain);
            bufferEnd = numRetain;
        }
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
        return indexOf(bytes.toArray(), minOffset, bytes.size(), query);
    }

    public int indexOf(byte [] bytes, int begIndex, int endIndex, byte [] query) {
        for (int i = begIndex; i < endIndex - query.length; i++) {
            if (bytesAre(bytes, i, endIndex, query)) {
                return i;
            }
        }
        return -1;
    }

    public boolean bytesAre(byte [] bytes, int offset, int endIndex, byte[] query) {
        if (offset + query.length > endIndex) {
            return false;
        }
        for (int i = 0; i < query.length; i++) {
            if (bytes[offset + i] != query[i]) {
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

    private int shiftBufferEndToFront(byte [] buffer, int bytesUsed, int numToShift) {
        int newLength = Math.min(numToShift, bytesUsed);
        System.arraycopy(buffer, bytesUsed - newLength, buffer, 0, newLength);
        return newLength;
    }
}
