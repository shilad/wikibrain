package org.wikapidia.matrix;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: shilad
 * Date: 4/19/13
 * Time: 10:29 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Matrix<T extends MatrixRow> extends Iterable<T> {
    <T extends MatrixRow> T getRow(int rowId) throws IOException;

    int[] getRowIds();

    int getNumRows();

    @Override
    Iterator<T> iterator();

    File getPath();
}
