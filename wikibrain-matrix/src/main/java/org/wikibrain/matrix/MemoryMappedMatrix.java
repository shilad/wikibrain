package org.wikibrain.matrix;

import gnu.trove.map.hash.TIntLongHashMap;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A wrapper around a file channel that contains a matrix in row major order.
 * Returns rows at a particular offset in the form of ByteBuffers backed by a memory mapped file.
 */
public class MemoryMappedMatrix {
    public static final Logger LOG = Logger.getLogger(MemoryMappedMatrix.class.getName());

    public static final int PAGE_SIZE = 1024 * 1024 * 1024;     // 1GB

    private FileChannel channel;
    protected List<MappedBufferWrapper> buffers = new ArrayList<MappedBufferWrapper>();
    private File path;

    private final IntBuffer rowIds;     // row ids in order of offsets on disk
    private final LongBuffer rowOffsets;      // row offsets associated with sorted ids
    private final int numRows;

    private WeakReference<int[]> rowIdsInDiskOrder = null;

    public MemoryMappedMatrix(File path, FileChannel channel,TIntLongHashMap rowOffsets) throws IOException {
        throw new UnsupportedOperationException();
    }

    public MemoryMappedMatrix(File path, FileChannel channel, IntBuffer rowIds, LongBuffer rowOffsets) throws IOException {
        this.path = path;
        this.channel = channel;
        if (rowIds.capacity() != rowOffsets.capacity()) {
            throw new IllegalArgumentException();
        }
        this.rowIds = rowIds;
        this.rowOffsets = rowOffsets;
        this.numRows = rowIds.capacity();
        int lastId = Integer.MIN_VALUE;
        for (int i = 0; i < numRows; i++) {
            if (rowIds.get(i) < lastId) {
                throw new IllegalArgumentException("Row ids must be in strictly increasing order");
            }
            lastId = rowIds.get(i);
        }
        pageInRows();
    }

    public void close() throws IOException {
        for (MappedBufferWrapper buffer : buffers) {
            buffer.close();
        }
        // try to garbage collect any freed buffers
        System.gc();
        System.gc();
        System.gc();
        channel.close();
    }

    private void pageInRows() throws IOException {
        if (numRows == 0) {
            return;
        }

        // tricky: pages must align with row boundaries
        int sortedIds[] = getRowIdsInDiskOrder();
        long startPos = getRowOffset(sortedIds[0]);
        long lastPos = startPos;

        for (int i = 1; i < numRows; i++) {
            long pos = getRowOffset(sortedIds[i]);
            if (pos - startPos > PAGE_SIZE) {
                assert(lastPos != startPos);
                addBuffer(startPos, lastPos);
                startPos = lastPos;
            }
            lastPos = pos;
        }
        addBuffer(startPos, channel.size());
    }


    private void addBuffer(long startPos, long endPos) throws IOException {
        long length = endPos - startPos;
        debug("adding page at " + startPos + " of length " + length);
        buffers.add(new MappedBufferWrapper(channel, startPos, endPos));
    }

    public ByteBuffer getRow(int rowId) throws IOException {
        long targetOffset = getRowOffset(rowId);
        if (targetOffset < 0) {
            return null;
        }
        MappedBufferWrapper row = null;
        // TODO: binary search
        for (int i = 0; i < buffers.size(); i++) {
            MappedBufferWrapper wrapper = buffers.get(i);
            if (wrapper.start <= targetOffset && targetOffset < wrapper.end) {
                row = wrapper;
            }
        }
        if (row == null) {
            throw new IllegalArgumentException("did not find row " + rowId + " with offset " + targetOffset);
        }
        return row.get(targetOffset);
    }

    private long getRowOffset(int rowId) {
        int lo = 0;
        int hi = numRows - 1;
        while (lo <= hi) {
            // Key is in a[lo..hi] or not present.
            int mid = (lo + hi) / 2;
            int midId = rowIds.get(mid);

            if (rowId < midId) {
                hi = mid - 1;
            } else if (rowId > midId) {
                lo = mid + 1;
            } else {
                return rowOffsets.get(mid);
            }
        }
        return -1;
    }

    static class MappedBufferWrapper {
        FileChannel channel;
        MappedByteBuffer buffer;
        long start;
        long end;

        public MappedBufferWrapper(FileChannel channel, long start, long end) {
            this.channel = channel;
            this.start = start;
            this.end = end;
        }
        public synchronized ByteBuffer get(long position) throws IOException {
            if (buffer == null) {
                buffer = channel.map(FileChannel.MapMode.READ_ONLY, start, end - start);
            }
            buffer.position((int) (position - start));
            return buffer.slice();
        }
        public synchronized void close() {
            buffer = null;
        }
    }

    public synchronized  int[] getRowIdsInDiskOrder() {
        if (rowIdsInDiskOrder == null || rowIdsInDiskOrder.get() == null) {
            int ids[] = new int[numRows];
            for (int i = 0; i < numRows; i++) {
                ids[i] = rowIds.get(i);
            }
            IntSorter.qsort(ids, new IntSorter.CompareInt() {
                @Override
                public boolean lessThan(int id1, int id2) {
                    return getRowOffset(id1) < getRowOffset(id2);
                }
            });
            this.rowIdsInDiskOrder = new WeakReference<int[]>(ids);
            return ids;
        } else {
            return rowIdsInDiskOrder.get();
        }
    }

    private void info(String message) {
        LOG.log(Level.INFO, "sparse matrix " + path + ": " + message);
    }
    private void debug(String message) {
        LOG.log(Level.FINEST, "sparse matrix " + path + ": " + message);
    }

}
