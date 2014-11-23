package org.wikibrain.matrix;

import gnu.trove.map.hash.TIntFloatHashMap;

import java.util.LinkedHashMap;

/**
 * A base class for matrix rows.
 * TODO: rename index to id. Indices are dense, ids are sparse.
 */
public abstract class BaseMatrixRow implements MatrixRow {
    @Override
    public abstract int getColIndex(int i);

    @Override
    public abstract float getColValue(int i);

    @Override
    public abstract int getRowIndex();

    @Override
    public abstract int getNumCols();

    @Override
    public LinkedHashMap<Integer, Float> asMap() {
        LinkedHashMap<Integer, Float> result = new LinkedHashMap<Integer, Float>();
        for (int i = 0; i < getNumCols(); i++) {
            result.put(getColIndex(i), getColValue(i));
        }
        return result;
    }

    @Override
    public TIntFloatHashMap asTroveMap() {
        TIntFloatHashMap result = new TIntFloatHashMap(getNumCols()*2);
        for (int i = 0; i < getNumCols(); i++) {
            result.put(getColIndex(i), getColValue(i));
        }
        return result;
    }

    @Override
    public double getNorm() {
        double length = 0.0;
        for (int i = 0; i < getNumCols(); i++) {
            length += getColValue(i) * getColValue(i);
        }
        return Math.sqrt(length);
    }

    @Override
    public int getIndexForId(int id) {
        for (int i = 0; i < getNumCols(); i++) {
            if (getColIndex(i) == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public float getValueForId(int id) {
        for (int i = 0; i < getNumCols(); i++) {
            if (getColIndex(i) == id) {
                return getColValue(i);
            }
        }
        return Float.NaN;
    }
}
