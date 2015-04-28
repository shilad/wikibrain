package org.wikibrain.matrix;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a sparse matrix.
 * The rows are memory mapped, so they can be immediately read from disk.
 */
public class SparseMatrix implements Matrix<SparseMatrixRow> {

    public static final Logger LOG = LoggerFactory.getLogger(SparseMatrix.class);

    // default header page size is 100MB, will be expanded if necessary
    public static final int DEFAULT_HEADER_SIZE = 100 * 1024 * 1024;

    public static final int FILE_HEADER = 0xabcdef;

    MemoryMappedMatrix rowBuffers;

    private int numRows = 0;
    private IntBuffer rowIds;       // row ids in sorted order
    private LongBuffer rowOffsets;  // file offsets associated with sorted row ids

    private FileChannel channel;
    private File path;


    private ValueConf vconf;

    public SparseMatrix(File path) throws IOException {
        this.path = path;
        if (!path.isFile()) {
            throw new IOException("File does not exist: " + path);
        }
        info("initializing sparse matrix with file length " + FileUtils.sizeOf(path));
        this.channel = (new FileInputStream(path)).getChannel();
        readHeaders();
        rowBuffers = new MemoryMappedMatrix(path, channel, rowIds, rowOffsets);
    }

    public long lastModified() {
        return path.lastModified();
    }

    private void readHeaders() throws IOException {
        long size = Math.min(channel.size(), DEFAULT_HEADER_SIZE);
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        if (buffer.getInt(0) != FILE_HEADER) {
            throw new IOException("invalid file header: " + buffer.getInt(0));
        }
        this.vconf = new ValueConf(buffer.getFloat(4), buffer.getFloat(8));
        this.numRows = buffer.getInt(12);
        int headerSize = 16 + 12*numRows;
        if (headerSize > DEFAULT_HEADER_SIZE) {
            info("maxPageSize not large enough for entire header. Resizing to " + headerSize);
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, headerSize);
        }

        debug("preparing buffer for " + numRows + " rows");
        buffer.position(16);
        buffer.limit(buffer.position() + 4 * numRows);
        rowIds = buffer.slice().asIntBuffer();
        if (rowIds.capacity() != numRows) {
            throw new IllegalStateException();
        }
        buffer.position(16 + 4 * numRows);
        buffer.limit(buffer.position() + 8 * numRows);
        rowOffsets = buffer.slice().asLongBuffer();
        if (rowOffsets.capacity() != numRows) {
            throw new IllegalStateException();
        }
    }


    @Override
    public SparseMatrixRow getRow(int rowId) throws IOException {
        ByteBuffer bb = rowBuffers.getRow(rowId);
        if (bb == null) {
            return null;
        } else {
            return new SparseMatrixRow(vconf, bb);
        }
    }

    @Override
    public int[] getRowIds() {
        return rowBuffers.getRowIdsInDiskOrder();
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    public ValueConf getValueConf() {
        return vconf;
    }

    @Override
    public Iterator<SparseMatrixRow> iterator() {
        return new SparseMatrixIterator();
    }

    public class SparseMatrixIterator implements Iterator<SparseMatrixRow> {
        private AtomicInteger i = new AtomicInteger();
        private int[] rowIds = rowBuffers.getRowIdsInDiskOrder();

        @Override
        public boolean hasNext() {
            return i.get() < numRows;
        }
        @Override
        public SparseMatrixRow next() {
            try {
                return (SparseMatrixRow)getRow(rowIds[i.getAndIncrement()]);
            } catch (IOException e) {
                LOG.error("getRow failed", e);
                return null;
            }
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public void close() throws IOException {
        rowBuffers.close();
    }

    @Override
    public File getPath() {
        return path;
    }

    private void info(String message) {
        LOG.info("sparse matrix " + path + ": " + message);
    }

    private void debug(String message) {
        LOG.debug("sparse matrix " + path + ": " + message);
    }
}
