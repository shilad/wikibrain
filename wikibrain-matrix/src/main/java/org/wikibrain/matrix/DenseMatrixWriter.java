package org.wikibrain.matrix;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntLongHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DenseMatrixWriter {

    public static final byte ROW_PADDING = Byte.MIN_VALUE;

    private static final Logger LOG = LoggerFactory.getLogger(DenseMatrixWriter.class);

    private File path;
    private TIntLongHashMap rowOffsets = new TIntLongHashMap();
    private TIntArrayList rowIndexes = new TIntArrayList();
    private File bodyPath;
    private BufferedOutputStream body;
    private long bodyOffset = 0;
    private ValueConf vconf;
    private int colIds[];

    public DenseMatrixWriter(File path, ValueConf conf) throws IOException {
        this.path = path;
        this.vconf = conf;
        info("writing matrix to " + path);

        // write tmp matrix file
        this.bodyPath = File.createTempFile("matrix", null);
        this.bodyPath.deleteOnExit();
        this.body = new BufferedOutputStream(new FileOutputStream(bodyPath));

        info("writing body to tmp file at " + bodyPath);
    }

    public ValueConf getValueConf() {
        return vconf;
    }

    public synchronized void writeRow(DenseMatrixRow row) throws IOException {
        if (!row.getValueConf().almostEquals(vconf)) {
            throw new IllegalArgumentException("Value conf for row does not match the writer's value conf");
        }
        if (colIds == null) {
            colIds = row.getColIds();
        }
        if (!Arrays.equals(colIds, row.getColIds())) {
            throw new IllegalArgumentException("Column id mismatch for row " + row.getRowIndex());
        }
        row.getBuffer().rewind();
        byte[] bytes = new byte[row.getBuffer().remaining()];
        row.getBuffer().get(bytes, 0, bytes.length);

        rowOffsets.put(row.getRowIndex(), bodyOffset);
        rowIndexes.add(row.getRowIndex());

        body.write(bytes);
        bodyOffset += bytes.length;

        // pad rows to 8 byte offsets to speed things up.
        while (bodyOffset % 8 != 0) {
            bodyOffset++;
            body.write(ROW_PADDING);
        }
    }

    public void finish() throws IOException {
        body.close();
        info("wrote " + bodyOffset + " bytes in body of matrix");

        // write offset file
        info("generating header");
        int sizeHeader = 16 + rowOffsets.size() * 12 + 4 + colIds.length * 4;
        body = new BufferedOutputStream(new FileOutputStream(path));
        body.write(intToBytes(DenseMatrix.FILE_HEADER));
        body.write(floatToBytes(vconf.minScore));
        body.write(floatToBytes(vconf.maxScore));
        body.write(intToBytes(rowOffsets.size()));
        body.write(intToBytes(colIds.length));

        // Next write row indexes in sorted order (4 bytes per row)
        int sortedIndexes[] = rowIndexes.toArray();
        Arrays.sort(sortedIndexes);
        for (int rowIndex : sortedIndexes) {
            body.write(intToBytes(rowIndex));
        }

        // Next write offsets for sorted indexes. (8 bytes per row)
        for (int rowIndex : sortedIndexes) {
            long rowOffset = rowOffsets.get(rowIndex);
            body.write(longToBytes(rowOffset + sizeHeader));
        }

        // Finally, write column ids
        for (int c : colIds) {
            body.write(intToBytes(c));
        }

        InputStream r = new FileInputStream(bodyPath);

        // append other file
        IOUtils.copyLarge(r, body);
        r.close();
        body.flush();
        body.close();

        info("wrote " + FileUtils.sizeOf(path) + " bytes to " + path);
    }

    private void info(String message) {
        LOG.info("dense matrix writer " + path + ": " + message);
    }

    public static void write(File file, Iterator<DenseMatrixRow> rows) throws IOException {
        write(file, rows, new ValueConf());
    }
    public static void write(File file, Iterator<DenseMatrixRow> rows, ValueConf vconf) throws IOException {
        DenseMatrixWriter w = new DenseMatrixWriter(file, vconf);
        while (rows.hasNext()) {
            w.writeRow(rows.next());
        }
        w.finish();
    }


    private static byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    private static byte[] longToBytes(long i) {
        return ByteBuffer.allocate(8).putLong(i).array();
    }
    private static byte[] floatToBytes(float f) {
        return ByteBuffer.allocate(4).putFloat(f).array();
    }
}
