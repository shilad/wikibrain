package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;

import java.io.IOException;
import java.util.logging.Logger;

public abstract class BaseUniversalSRMetric implements UniversalSRMetric{
    private static Logger LOG = Logger.getLogger(BaseLocalSRMetric.class.getName());
    protected int numThreads = Runtime.getRuntime().availableProcessors();

    protected LocalSRMetric localSRMetric;

    protected SparseMatrix mostSimilarUniversalMatrix;

    BaseUniversalSRMetric(LocalSRMetric localSRMetric){
        this.localSRMetric=localSRMetric;
    }

    public void setMostSimilarUniversalMatrix(SparseMatrix matrix) {
        this.mostSimilarUniversalMatrix = matrix;
    }

    public boolean hasCachedMostSimilarUniversal(int wpId) throws IOException {
        return mostSimilarUniversalMatrix != null && mostSimilarUniversalMatrix.getRow(wpId) != null;
    }

    public SRResultList getCachedMostSimilarUniversal(int wpId, int numResults, TIntSet validIds) throws IOException {
        if (!hasCachedMostSimilarUniversal(wpId)){
            return null;
        }
        SparseMatrixRow row = mostSimilarUniversalMatrix.getRow(wpId);
        int n = 0;
        SRResultList srl = new SRResultList(row.getNumCols());
        for (int i = 0;i < row.getNumCols() &&  n < numResults; i++) {
            int wpId2 = row.getColIndex(i);
            if (validIds == null || validIds.contains(wpId2)) {
                srl.set(n++, row.getColIndex(i), row.getColValue(i));
            }
        }
        srl.truncate(n);
        return srl;
    }

    @Override
    public abstract SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations);

    @Override
    public abstract SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations);


    @Override
    public abstract SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations);

    @Override
    public abstract SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations, TIntSet validIds);

    @Override
    public abstract SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations);

    @Override
    public abstract SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations, TIntSet validIds);

    @Override
    public abstract double[][] cosimilarity(int[] rowIds, int[] colIds) throws IOException;

    @Override
    public abstract double[][] cosimilarity(LocalString[] rowPhrases, LocalString[] colPhrases) throws IOException;

    @Override
    public double[][] cosimilarity(int[] ids) throws IOException {
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=1;
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]=similarity(
                        new UniversalPage(ids[i], 0, null, null),
                        new UniversalPage(ids[j], 0, null, null),
                        false).getValue();
            }
        }
        for (int i=1; i<ids.length; i++){
            for (int j=i-1; j>-1; j--){
                cos[i][j]=cos[j][i];
            }
        }
        return cos;
    }

    @Override
    public abstract double[][] cosimilarity(LocalString[] phrases) throws IOException;
}
