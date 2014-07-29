package org.wikibrain.matrix;

import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestUtils {
    /**
    /**
     * Creates a new random matrix with nRows rows.
     * Each row has a random length chosen uniformly from 0 to maxRowLen.
     * If sameIds is true, the column ids are chosen from the row ids.
     * @param nRows
     * @param maxRowLen
     * @param sameIds
     * @return
     */
    public static List<SparseMatrixRow> createSparseTestMatrixRows(int nRows, int maxRowLen, boolean sameIds) throws IOException {
        return createSparseTestMatrixRowsInternal(nRows, maxRowLen, sameIds, null);
    }
    public static List<DenseMatrixRow> createDenseTestMatrixRows(int nRows, int numCols) throws IOException {
        return createDenseTestMatrixRowsInternal(nRows, numCols, null);
    }


    /**
     * Creates a new random matrix with nRows rows.
     * Each row has a random length chosen uniformly from 0 to maxRowLen.
     * If sameIds is true, the column ids are chosen from the row ids.
     * @param nRows
     * @param maxRowLen
     * @param sameIds
     * @return
     */
    public static SparseMatrix createSparseTestMatrix(int nRows, int maxRowLen, boolean sameIds) throws IOException {
        File tmpFile = File.createTempFile("matrix", null);
        tmpFile.deleteOnExit();
        SparseMatrixWriter writer = new SparseMatrixWriter(tmpFile, new ValueConf());
        createSparseTestMatrixRowsInternal(nRows, maxRowLen, sameIds, writer);
        writer.finish();
        return new SparseMatrix(tmpFile);
    }

    /*
    public static DenseMatrix createDenseTestMatrix(int nRows, int numCols) throws IOException {
        return createDenseTestMatrix(nRows, numCols, SparseMatrix.DEFAULT_LOAD_ALL_PAGES, SparseMatrix.DEFAULT_MAX_PAGE_SIZE);
    }
    public static DenseMatrix createDenseTestMatrix(int nRows, int numCols, boolean readAllRows, int pageSize) throws IOException {
        File tmpFile = File.createTempFile("matrix", null);
        tmpFile.deleteOnExit();
        DenseMatrixWriter writer = new DenseMatrixWriter(tmpFile, new ValueConf());
        createDenseTestMatrixRowsInternal(nRows, numCols, writer);
        writer.finish();
        return new DenseMatrix(tmpFile, readAllRows, pageSize);
    }
    */


    /**
     * Either writes or returns the sparse matrix rows depending on whether the writer is passed.
     * @param nRows
     * @param maxRowLen
     * @param sameIds
     * @param writer
     * @return if writer == null the list of rows, else null
     */
    private static List<SparseMatrixRow> createSparseTestMatrixRowsInternal(
            int nRows, int maxRowLen, boolean sameIds, SparseMatrixWriter writer)
            throws IOException {
        Random random = new Random();
        List<SparseMatrixRow> rows = new ArrayList<SparseMatrixRow>();
        int rowIds[] = pickIds(nRows, nRows * 2);
        for (int id1 : rowIds) {
            LinkedHashMap<Integer, Float> data = new LinkedHashMap<Integer, Float>();
            int numCols = Math.max(1, random.nextInt(maxRowLen));
            int colIds[] = sameIds ? pickIdsFrom(rowIds, numCols) : pickIds(numCols, maxRowLen * 2);
            for (int id2 : colIds) {
                data.put(id2, random.nextFloat());
            }
            SparseMatrixRow row = new SparseMatrixRow(new ValueConf(), id1, data);
            if (writer == null) {
                rows.add(row);
            } else {
                writer.writeRow(row);
            }
        }
        return (writer == null) ? rows : null;
    }

    /**
     * Either writes or returns the sparse matrix rows depending on whether the writer is passed.
     * @param nRows
     * @param numCols
     * @param writer
     * @return if writer == null the list of rows, else null
     */
    private static List<DenseMatrixRow> createDenseTestMatrixRowsInternal(
            int nRows, int numCols, DenseMatrixWriter writer)
            throws IOException {
        Random random = new Random();
        List<DenseMatrixRow> rows = new ArrayList<DenseMatrixRow>();
        int rowIds[] = pickIds(nRows, nRows * 2);
        int colIds[] = pickIds(numCols, numCols * 2);
        Arrays.sort(colIds);
        for (int id1 : rowIds) {
            LinkedHashMap<Integer, Float> data = new LinkedHashMap<Integer, Float>();
            for (int id2 : colIds) {
                data.put(id2, random.nextFloat());
            }
            DenseMatrixRow row = new DenseMatrixRow(new ValueConf(), id1, data);
            if (writer == null) {
                rows.add(row);
            } else {
                writer.writeRow(row);
            }
        }
        return (writer == null) ? rows : null;
    }


    /**
     * Returns a set of n unique ids from 1 through maxId in random order.
     * @param n
     * @param maxId
     * @return
     */
    public static int[] pickIds(int n, int maxId) {
        assert(n < maxId);
        Random random = new Random();
        TIntHashSet picked = new TIntHashSet();
        for (int i = 0; i < n; i++) {
            while (true) {
                int id = random.nextInt(maxId - 1) + 1;
                if (!picked.contains(id)) {
                    picked.add(id);
                    break;
                }
            }
        }
        return picked.toArray();
    }

    /**
     * Selects n random unique ids from the array of ids.
     * @param ids
     * @param n
     * @return
     */
    public static int[] pickIdsFrom(int ids[], int n) {
        assert(ids.length >= n);
        Random random = new Random();
        TIntHashSet picked = new TIntHashSet();
        for (int i = 0; i < n; i++) {
            while (true) {
                int id = ids[random.nextInt(ids.length)];
                if (!picked.contains(id)) {
                    picked.add(id);
                    break;
                }
            }
        }
        return picked.toArray();
    }
}
