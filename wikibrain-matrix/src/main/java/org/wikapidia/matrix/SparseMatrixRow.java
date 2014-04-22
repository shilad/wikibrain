package org.wikapidia.matrix;

import gnu.trove.map.TIntFloatMap;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * A single sparse matrix row backed by a byte buffer. The row contains:
 * - a row id (int),
 * - a set of n columns, each with an id (int) and value (float packed into two bytes)
 *
 * The row can either be created from the component data, or from a byte buffer.
 * This means that the object can wrap data from an mmap'd file in the correct format.
 */
public final class SparseMatrixRow extends BaseMatrixRow implements MatrixRow {
    Logger LOG = Logger.getLogger(SparseMatrixRow.class.getName());
    public static final Float MIN_SCORE = -1.1f;
    public static final Float MAX_SCORE = 1.1f;

    public static final Float SCORE_RANGE = (MAX_SCORE - MIN_SCORE);
    public static final int PACKED_RANGE = (Short.MAX_VALUE - Short.MIN_VALUE);

    public static final int HEADER = 0xfefefefe;

    /**
     * The main "source" buffer.
     */
    private ByteBuffer buffer;

    /**
     * A view buffer that points to the header.
     */
    private IntBuffer headerBuffer;

    /**
     * A view buffer that points to the ids.
     */
    private IntBuffer idBuffer;

    /**
     * A view buffer that points to the values.
     */
    private ShortBuffer valBuffer;
    private ValueConf vconf;

    public SparseMatrixRow(ValueConf vconf, int rowIndex, TIntFloatMap row) {
        this(vconf, rowIndex, row.keys(), row.values());
    }

    public SparseMatrixRow(ValueConf vconf, int rowIndex, LinkedHashMap<Integer, Float> row) {
        this(vconf, rowIndex,
            ArrayUtils.toPrimitive(row.keySet().toArray(new Integer[] {})),
            ArrayUtils.toPrimitive(row.values().toArray(new Float[]{}))
        );
    }

    public SparseMatrixRow(ValueConf vconf, int rowIndex, int colIds[], float colVals[]) {
        this.vconf = vconf;
        short packed[] = new short[colVals.length];
        for (int i = 0; i < colVals.length; i++) {
            packed[i] = vconf.pack(colVals[i]);
        }
        createBuffer(rowIndex, colIds, packed);
    }

    public SparseMatrixRow(ValueConf vconf, int rowIndex, int colIds[], short colVals[]) {
        this.vconf = vconf;
        createBuffer(rowIndex, colIds, colVals);
    }

    public void createBuffer(int rowIndex, int colIds[], short colVals[]) {
        assert(colIds.length == colVals.length);

        buffer = ByteBuffer.allocate(
                4 +                 // header
                4 +                 // row index
                4 +                 // num cols
                4 * colVals.length +    // col indexes
                2 * colVals.length      // col values
        );
        createViewBuffers(colVals.length);

        headerBuffer.put(0, HEADER);
        headerBuffer.put(1, rowIndex);
        headerBuffer.put(2, colVals.length);
        idBuffer.put(colIds, 0, colIds.length);
        valBuffer.put(colVals, 0, colVals.length);
    }

    private void createViewBuffers(int numColumns) {
        buffer.position(0);
        headerBuffer = buffer.asIntBuffer();
        buffer.position(3 * 4);
        idBuffer = buffer.asIntBuffer();
        buffer.position(3 * 4 + numColumns * 4);
        valBuffer = buffer.asShortBuffer();
    }

    /**
     * Wrap an existing byte buffer that contains a row.
     * @param buffer
     */
    public SparseMatrixRow(ValueConf vconf, ByteBuffer buffer) {
        this.vconf = vconf;
        this.buffer = buffer;
        if (this.buffer.getInt(0) != HEADER) {
            throw new IllegalArgumentException("Invalid header in byte buffer");
        }
        createViewBuffers(buffer.getInt(8));
    }

    @Override
    public final int getColIndex(int i) {
        return idBuffer.get(i);
    }

    @Override
    public final float getColValue(int i) {
        return vconf.unpack(valBuffer.get(i));
    }

    public final short getPackedColValue(int i) {
        return valBuffer.get(i);
    }

    @Override
    public final int getRowIndex() {
        return headerBuffer.get(1);
    }

    @Override
    public final int getNumCols() {
        return headerBuffer.get(2);
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public ValueConf getValueConf() {
        return vconf;
    }
}
