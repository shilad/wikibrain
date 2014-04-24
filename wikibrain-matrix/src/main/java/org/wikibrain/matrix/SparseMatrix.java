package org.wikibrain.matrix;

import gnu.trove.map.hash.TIntLongHashMap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a sparse matrix.
 * The rows are memory mapped, so they can be immediately read from disk.
 */
public class SparseMatrix implements Matrix<SparseMatrixRow> {

    public static final Logger LOG = Logger.getLogger(SparseMatrix.class.getName());

    // default header page size is 100MB, will be expanded if necessary
    public static final int DEFAULT_HEADER_SIZE = 100 * 1024 * 1024;

    public static final int FILE_HEADER = 0xabcdef;

    MemoryMappedMatrix rowBuffers;

    private TIntLongHashMap rowOffsets = new TIntLongHashMap();
    private int rowIds[];
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
        rowBuffers = new MemoryMappedMatrix(path, channel, rowOffsets);
    }

    private void readHeaders() throws IOException {
        long size = Math.min(channel.size(), DEFAULT_HEADER_SIZE);
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        if (buffer.getInt(0) != FILE_HEADER) {
            throw new IOException("invalid file header: " + buffer.getInt(0));
        }
        this.vconf = new ValueConf(buffer.getFloat(4), buffer.getFloat(8));
        int numRows = buffer.getInt(12);
        int headerSize = 16 + 12*numRows;
        if (headerSize > DEFAULT_HEADER_SIZE) {
            info("maxPageSize not large enough for entire header. Resizing to " + headerSize);
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, headerSize);
        }
        debug("reading offsets for " + numRows + " rows");
        rowIds = new int[numRows];
        rowOffsets.ensureCapacity(numRows);
        for (int i = 0; i < numRows; i++) {
            int pos = 16 + 12 * i;
            int rowIndex = buffer.getInt(pos);
            long rowOffset = buffer.getLong(pos + 4);
            rowOffsets.put(rowIndex, rowOffset);
//            debug("adding row index " + rowIndex + " at offset " + rowOffset);
            rowIds[i] = rowIndex;
        }
        debug("read " + numRows + " offsets");
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
        return rowIds;
    }

    @Override
    public int getNumRows() {
        return rowIds.length;
    }

    public ValueConf getValueConf() {
        return vconf;
    }

    public void dump() throws IOException {
        for (int id : rowIds) {
            System.out.print("" + id + ": ");
            MatrixRow row = getRow(id);
            for (int i = 0; i < row.getNumCols(); i++) {
                int id2 = row.getColIndex(i);
                float val = row.getColValue(i);
                System.out.print(" " + id2 + "=" + val);
            }
            System.out.println();
        }
    }

    @Override
    public Iterator<SparseMatrixRow> iterator() {
        return new SparseMatrixIterator();
    }

    public class SparseMatrixIterator implements Iterator<SparseMatrixRow> {
        private int i = 0;
        @Override
        public boolean hasNext() {
            return i < rowIds.length;
        }
        @Override
        public SparseMatrixRow next() {
            try {
                return (SparseMatrixRow)getRow(rowIds[i++]);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "getRow failed", e);
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
        LOG.log(Level.INFO, "sparse matrix " + path + ": " + message);
    }

    private void debug(String message) {
        LOG.log(Level.FINE, "sparse matrix " + path + ": " + message);
    }
}
