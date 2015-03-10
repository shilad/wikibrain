package org.wikibrain.matrix;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Base class for dense / sparse matrices
 * @author Shilad Sen
 */
public interface Matrix<T extends MatrixRow> extends Iterable<T>, Closeable {
    /**
     * Gets a particular matrix row.
     * @param rowId
     * @return The row, or null if it does not exist.
     * @throws IOException
     */
    T getRow(int rowId) throws IOException;

    /**
     * Gets all row ids.
     * @return
     */
    int[] getRowIds();

    /**
     * Returns the number of rows.
     * @return
     */
    int getNumRows();

    /**
     * Returns the path to the file that backs the matrix.
     * @return
     */
    File getPath();
}
