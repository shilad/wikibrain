package org.wikibrain.matrix;

import gnu.trove.map.hash.TIntLongHashMap;
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
 * Implementation of a dense matrix.
 * The rows are memory mapped, so they can be immediately read from disk.
 * All rows must have the same columns in the same order.
 */
public class DenseMatrix implements Matrix<DenseMatrixRow> {

    public static final Logger LOG = LoggerFactory.getLogger(DenseMatrix.class);

    public static final int FILE_HEADER = 0xabccba;

    private int numRows;
    private IntBuffer rowIds;
    private LongBuffer rowOffsets;

    private int colIds[];
    private FileChannel channel;
    private File path;

    MemoryMappedMatrix rowBuffers;
    private ValueConf vconf;

    // default header page size is 100MB, will be expanded if necessary
    public static final int DEFAULT_HEADER_SIZE = 100 * 1024 * 1024;

    /**
     * Create a dense matrix based on the data in a particular file.
     * @param path Path to the matrix data file.
     * @throws java.io.IOException
     */
    public DenseMatrix(File path) throws IOException {
        this.path = path;
        info("initializing sparse matrix with file length " + FileUtils.sizeOf(path));
        this.channel = (new FileInputStream(path)).getChannel();
        readHeaders();
        rowBuffers = new MemoryMappedMatrix(path, channel, rowIds, rowOffsets);
    }

    private void readHeaders() throws IOException {
        long size = Math.min(channel.size(), DEFAULT_HEADER_SIZE);
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);

        // read header
        if (buffer.getInt(0) != FILE_HEADER) {
            throw new IOException("invalid file header: " + buffer.getInt(0));
        }
        this.vconf = new ValueConf(buffer.getFloat(4), buffer.getFloat(8));
        this.numRows = buffer.getInt(12);
        int numCols = buffer.getInt(16);
        int headerSize = 20 + 12 * numRows + 4 * numCols;
        if (headerSize > DEFAULT_HEADER_SIZE) {
            info("maxPageSize not large enough for entire header. Resizing to " + headerSize);
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, headerSize);
        }

        debug("preparing buffer for " + numRows + " rows");
        buffer.position(20);
        buffer.limit(buffer.position() + 4 * numRows);
        rowIds = buffer.slice().asIntBuffer();
        if (rowIds.capacity() != numRows) {
            throw new IllegalStateException();
        }
        buffer.position(20 + 4 * numRows);
        buffer.limit(buffer.position() + 8 * numRows);
        rowOffsets = buffer.slice().asLongBuffer();
        if (rowOffsets.capacity() != numRows) {
            throw new IllegalStateException();
        }

        // read column ids
        buffer.limit(headerSize);
        int pos = 20 + 12 * numRows;
        colIds = new int[numCols];
        for (int i = 0; i < numCols; i++) {
            colIds[i] = buffer.getInt(pos);
            pos += 4;
        }
        if (!SparseMatrixUtils.isIncreasing(colIds)) {
            throw new IllegalArgumentException("Columns must be sorted by id");
        }
        info("read " + colIds.length + " column ids");
    }

    @Override
    public DenseMatrixRow getRow(int rowId) throws IOException {
        ByteBuffer bb = rowBuffers.getRow(rowId);
        if (bb == null) {
            return null;
        } else {
            return new DenseMatrixRow(vconf, colIds, bb);
        }
    }

    @Override
    public int[] getRowIds() {
        return rowBuffers.getRowIdsInDiskOrder();
    }

    public int[] getColIds() {
        return colIds;
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    public ValueConf getValueConf() {
        return vconf;
    }


    @Override
    public Iterator<DenseMatrixRow> iterator() {
        return new DenseMatrixIterator();
    }

    public class DenseMatrixIterator implements Iterator<DenseMatrixRow> {
        private AtomicInteger i = new AtomicInteger();
        private int[] rowIds = rowBuffers.getRowIdsInDiskOrder();
        @Override
        public boolean hasNext() {
            return i.get() < numRows;
        }
        @Override
        public DenseMatrixRow next() {
            try {
                return getRow(rowIds[i.getAndIncrement()]);
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

    @Override
    public File getPath() {
        return path;
    }

    @Override
    public void close() throws IOException {
        rowBuffers.close();
    }

    private void info(String message) {
        LOG.error("dense matrix " + path + ": " + message);
    }

    private void debug(String message) {
        LOG.error("dense matrix " + path + ": " + message);
    }
}
