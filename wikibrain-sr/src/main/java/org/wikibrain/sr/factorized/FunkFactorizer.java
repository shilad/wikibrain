package org.wikibrain.sr.factorized;

import gnu.trove.map.TIntObjectMap;
import no.uib.cipr.matrix.DenseVectorSub;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.*;
import org.wikibrain.matrix.SparseMatrix;

import java.util.Map;

/**
 * @author Shilad Sen
 */
public class FunkFactorizer implements Factorizer {
    @Override
    public float[][] factorize(SparseMatrix adjacencies, int rank) {
        System.err.println("here 1: ");
        Matrix m = FactorizerUtils.toMTJ(adjacencies);
        System.out.println("dims are " + m.numRows() + ", " + m.numColumns());
        Matrix m3 = new LinkedSparseMatrix(m) {};
        System.err.println("here 2");
        m = m.scale(0.5);
        System.err.println("here 3");
        m3.transpose();
        m.add(0.5, m3);
        System.err.println("here 4");
        ArpackSym sym = new ArpackSym(m);
        System.err.println("here 5");
        Map<Double, DenseVectorSub> factors = sym.solve(5, ArpackSym.Ritz.LM);
        System.err.println("here 6");
        float[][] result = new float[m.numRows()][rank];
        int i = 0;
        for (Double d : factors.keySet()) {
            DenseVectorSub v = factors.get(d);
            for (int j = 0; j < v.size(); j++) {
                result[j][i] = (float) v.get(j);
            }
            i++;
        }
        return result;
    }
}
