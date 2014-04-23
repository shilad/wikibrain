package org.wikibrain.matrix;

import gnu.trove.map.hash.TIntFloatHashMap;

import java.util.LinkedHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: shilad
 * Date: 4/19/13
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MatrixRow {
    int getColIndex(int i);

    float getColValue(int i);

    int getRowIndex();

    int getNumCols();

    LinkedHashMap<Integer, Float> asMap();

    TIntFloatHashMap asTroveMap();

    double getNorm();

    int getIndexForId(int id);

    float getValueForId(int id);
}
