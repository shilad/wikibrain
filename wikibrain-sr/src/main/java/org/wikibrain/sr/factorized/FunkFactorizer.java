package org.wikibrain.sr.factorized;

import gnu.trove.map.TIntObjectMap;
import no.uib.cipr.matrix.sparse.ArpackSym;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import org.wikibrain.matrix.SparseMatrix;

/**
 * @author Shilad Sen
 */
public class FunkFactorizer implements Factorizer {
    @Override
    public float[][] factorize(SparseMatrix adjacencies, int rank) {
        FlexCompRowMatrix m = FactorizerUtils.toMTJ(adjacencies);
        ArpackSym sym = new ArpackSym(m);
        sym.solve(5, ArpackSym.Ritz.LM);
        return null;
    }
}
