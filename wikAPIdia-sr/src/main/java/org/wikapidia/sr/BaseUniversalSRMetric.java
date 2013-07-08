package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.disambig.Disambiguator;

import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Logger;

public abstract class BaseUniversalSRMetric implements UniversalSRMetric{
    private static Logger LOG = Logger.getLogger(BaseUniversalSRMetric.class.getName());
    protected int numThreads = Runtime.getRuntime().availableProcessors();
    protected UniversalPageDao universalPageDao;
    protected Disambiguator disambiguator;
    protected int algorithmId;


    protected SparseMatrix mostSimilarUniversalMatrix;

    public BaseUniversalSRMetric(Disambiguator disambiguator, UniversalPageDao universalPageDao, int algorithmId){
        this.universalPageDao = universalPageDao;
        this.disambiguator = disambiguator;
        this.algorithmId = algorithmId;
    }

    public void setMostSimilarUniversalMatrix(SparseMatrix matrix) {
        this.mostSimilarUniversalMatrix = matrix;
    }

    public boolean hasCachedMostSimilarUniversal(int wpId){
        boolean hasCached;
        try {
            hasCached = mostSimilarUniversalMatrix != null && mostSimilarUniversalMatrix.getRow(wpId) != null;
        } catch (IOException e) {
            hasCached = false;
        }
        return hasCached;
    }

    public SRResultList getCachedMostSimilarUniversal(int wpId, int numResults, TIntSet validIds){
        if (!hasCachedMostSimilarUniversal(wpId)){
            return null;
        }
        SparseMatrixRow row;
        try {
            row = mostSimilarUniversalMatrix.getRow(wpId);
        } catch (IOException e) {
            return null;
        }
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
    public abstract SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations) throws DaoException;

    @Override
    public SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations) throws DaoException {
        HashSet<LocalString> context = new HashSet<LocalString>();
        context.add(phrase2);
        LocalId similar1 = disambiguator.disambiguate(phrase1, context);
        int uId1 = universalPageDao.getUnivPageId(similar1.asLocalPage(),algorithmId);
        UniversalPage up1 = universalPageDao.getById(uId1,algorithmId);
        context.clear();
        context.add(phrase1);
        LocalId similar2 = disambiguator.disambiguate(phrase2, context);
        int uId2 = universalPageDao.getUnivPageId(similar2.asLocalPage(),algorithmId);
        UniversalPage up2 = universalPageDao.getById(uId1,algorithmId);
        return similarity(up1,up2,explanations);

    }


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
    public double[][] cosimilarity(int[] ids) throws IOException, DaoException {
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
