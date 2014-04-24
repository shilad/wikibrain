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
 * Implementation of a dense matrix.
 * The rows are memory mapped, so they can be immediately read from disk.
 * All rows must have the same columns in the same order.
 */
public class DenseMatrix implements Matrix<DenseMatrixRow> {

    public static final Logger LOG = Logger.getLogger(DenseMatrix.class.getName());

    public static final int FILE_HEADER = 0xabccba;

    private TIntLongHashMap rowOffsets = new TIntLongHashMap();
    private int rowIds[];
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
        rowBuffers = new MemoryMappedMatrix(path, channel, rowOffsets);
    }

    private void readHeaders() throws IOException {
        int pos = 0;
        long size = Math.min(channel.size(), DEFAULT_HEADER_SIZE);
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);

        // read header
        if (buffer.getInt(pos) != FILE_HEADER) {
            throw new IOException("invalid file header: " + buffer.getInt(pos));
        }
        pos += 4;
        this.vconf = new ValueConf(buffer.getFloat(pos), buffer.getFloat(pos + 4));
        pos += 8;
        int numRows = buffer.getInt(pos);
        pos += 4;

        // read row ids and offsets
        info("reading offsets for " + numRows + " rows");
        rowIds = new int[numRows];
        for (int i = 0; i < numRows; i++) {
            int rowIndex = buffer.getInt(pos);
            long rowOffset = buffer.getLong(pos + 4);
            rowOffsets.put(rowIndex, rowOffset);
//            debug("adding row index " + rowIndex + " at offset " + rowOffset);
            rowIds[i] = rowIndex;
            pos += 12;
        }
        info("read " + numRows + " offsets");

        // read column ids
        int numCols = buffer.getInt(pos);
        info("reading ids for " + numCols + " cols");
        pos += 4;
        colIds = new int[numCols];
        for (int i = 0; i < numCols; i++) {
            colIds[i] = buffer.getInt(pos);
            pos += 4;
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
        return rowIds;
    }

    public int[] getColIds() {
        return colIds;
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
    public Iterator<DenseMatrixRow> iterator() {
        return new DenseMatrixIterator();
    }

    public class DenseMatrixIterator implements Iterator<DenseMatrixRow> {
        private int i = 0;
        @Override
        public boolean hasNext() {
            return i < rowIds.length;
        }
        @Override
        public DenseMatrixRow next() {
            try {
                return getRow(rowIds[i++]);
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

    @Override
    public File getPath() {
        return path;
    }

    @Override
    public void close() throws IOException {
        rowBuffers.close();
    }

    private void info(String message) {
        LOG.log(Level.WARNING, "dense matrix " + path + ": " + message);
    }

    private void debug(String message) {
        LOG.log(Level.FINEST, "dense matrix " + path + ": " + message);
    }
}
