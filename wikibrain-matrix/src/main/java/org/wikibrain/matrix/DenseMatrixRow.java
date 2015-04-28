package org.wikibrain.matrix;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single dense matrix row backed by a byte buffer. The row contains:
 * - a row id (int),
 * - a set of n columns, each with a value (float packed into two bytes)
 *
 * Since the matrix is dense, the row assumes that a single copy of column ids is
 * stored somewhere in the container matrix.
 *
 * The row can either be created from the component data, or from a byte buffer.
 * This means that the object can wrap data from an mmap'd file in the correct format.
 */
public final class DenseMatrixRow extends BaseMatrixRow implements MatrixRow {
    private static final Logger LOG = LoggerFactory.getLogger(DenseMatrixRow.class);

    public static final Float MIN_SCORE = -1.1f;
    public static final Float MAX_SCORE = 1.1f;

    public static final Float SCORE_RANGE = (MAX_SCORE - MIN_SCORE);
    public static final int PACKED_RANGE = (Short.MAX_VALUE - Short.MIN_VALUE);

    public static final int HEADER = 0xfefefefa;
    private final float c1;
    private final float c2;

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
    private int[] colIds;

    /**
     * A view buffer that points to the values.
     */
    private ShortBuffer valBuffer;

    private ValueConf vconf;

    public DenseMatrixRow(ValueConf vconf, int rowIndex, LinkedHashMap<Integer, Float> row) {
        this(vconf, rowIndex,
            ArrayUtils.toPrimitive(row.keySet().toArray(new Integer[] {})),
            ArrayUtils.toPrimitive(row.values().toArray(new Float[]{}))
        );
    }

    public DenseMatrixRow(ValueConf vconf, int rowIndex, int colIds[], float colVals[]) {
        if (!SparseMatrixUtils.isIncreasing(colIds)) {
            throw new IllegalArgumentException("Columns must be sorted by id");
        }
        this.vconf = vconf;
        this.c1 = vconf.c1;
        this.c2 = vconf.c2;
        this.colIds = colIds;
        short packed[] = new short[colVals.length];
        for (int i = 0; i < colVals.length; i++) {
            packed[i] = vconf.pack(colVals[i]);
        }
        createBuffer(rowIndex, colIds, packed);
    }

    public void createBuffer(int rowIndex, int colIds[], short colVals[]) {
        assert(colIds.length == colVals.length);
        this.colIds = colIds;

        buffer = ByteBuffer.allocate(
                4 +                 // header
                4 +                 // row index
                2 * colVals.length  // col values
        );
        createViewBuffers(colVals.length);

        headerBuffer.put(0, HEADER);
        headerBuffer.put(1, rowIndex);
        valBuffer.put(colVals, 0, colVals.length);
    }

    private void createViewBuffers(int numColumns) {
        buffer.position(0);
        headerBuffer = buffer.asIntBuffer();
        buffer.position(2 * 4);
        valBuffer = buffer.asShortBuffer();
    }

    /**
     * Wrap an existing byte buffer that contains a row.
     * @param colIds
     * @param buffer
     */
    public DenseMatrixRow(ValueConf vconf, int colIds[], ByteBuffer buffer) {
        if (!SparseMatrixUtils.isIncreasing(colIds)) {
            throw new IllegalArgumentException("Columns must be sorted by id");
        }
        this.vconf = vconf;
        this.colIds = colIds;
        this.buffer = buffer;
        this.c1 = vconf.c1;
        this.c2 = vconf.c2;
        if (this.buffer.getInt(0) != HEADER) {
            throw new IllegalArgumentException("Invalid header in byte buffer");
        }
        createViewBuffers(buffer.getInt(8));
    }

    public final double dot(float [] vector) {
        if (vector.length != colIds.length) throw new IllegalArgumentException();
        double sum = 0.0;
        for (int i = 0; i < vector.length; i++) {
            sum += vector[i] * (c1 * valBuffer.get(i) + c2);
        }
        return sum;
    }

    public final double dot(DenseMatrixRow X) {
        double sum = 0.0;
        for (int i = 0; i < X.colIds.length; i++) {
            sum += (c1 * X.valBuffer.get(i) + c2) * (c1 * valBuffer.get(i) + c2);
        }
        return sum;
    }

    @Override
    public final int getColIndex(int i) {
        return colIds[i];
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
        return colIds.length;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public ValueConf getValueConf() {
        return vconf;
    }

    protected int[] getColIds() {
        return colIds;
    }

    public float[] getValues() {
        float vals[] = new float[colIds.length];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = getColValue(i);
        }
        return vals;
    }
}
