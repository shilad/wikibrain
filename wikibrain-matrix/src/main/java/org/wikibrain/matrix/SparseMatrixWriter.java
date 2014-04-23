package org.wikibrain.matrix;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntLongHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SparseMatrixWriter {

    public static final byte ROW_PADDING = Byte.MIN_VALUE;

    private static final Logger LOG = Logger.getLogger(SparseMatrixWriter.class.getName());

    private File path;
    private TIntLongHashMap rowOffsets = new TIntLongHashMap();
    private TIntArrayList rowIndexes = new TIntArrayList();
    private File bodyPath;
    private BufferedOutputStream body;
    private long bodyOffset = 0;
    private ValueConf vconf;

    public SparseMatrixWriter(File path, ValueConf conf) throws IOException {
        this.path = path;
        this.vconf = conf;
        info("writing matrix to " + path);

        // write tmp matrix file
        this.bodyPath = File.createTempFile("matrix", null);
        this.bodyPath.deleteOnExit();
        this.body = new BufferedOutputStream(new FileOutputStream(bodyPath));

        info("writing body to tmp file at " + bodyPath);
    }

    public synchronized void writeRow(SparseMatrixRow row) throws IOException {
        if (!row.getValueConf().almostEquals(vconf)) {
            throw new IllegalArgumentException("Value conf for row does not match the writer's value conf");
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
        int sizeHeader = 16 + rowOffsets.size() * 12;
        body = new BufferedOutputStream(new FileOutputStream(path));
        body.write(intToBytes(SparseMatrix.FILE_HEADER));
        body.write(floatToBytes(vconf.minScore));
        body.write(floatToBytes(vconf.maxScore));
        body.write(intToBytes(rowOffsets.size()));
        for (int i = 0; i < rowIndexes.size(); i++) {
            int rowIndex = rowIndexes.get(i);
            long rowOffset = rowOffsets.get(rowIndex);
            body.write(intToBytes(rowIndex));
            body.write(longToBytes(rowOffset + sizeHeader));
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
        LOG.log(Level.INFO, "sparse matrix writer " + path + ": " + message);
    }

    public static void write(File file, Iterator<SparseMatrixRow> rows) throws IOException {
        write(file, rows, new ValueConf());
    }
    public static void write(File file, Iterator<SparseMatrixRow> rows, ValueConf vconf) throws IOException {
        SparseMatrixWriter w = new SparseMatrixWriter(file, vconf);
        while (rows.hasNext()) {
            w.writeRow(rows.next());
        }
        w.finish();
    }

    public ValueConf getValueConf() {
        return vconf;
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
