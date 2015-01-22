package org.wikibrain.sr.factorized;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.sr.BaseSRMetric;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.utils.WbMathUtils;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class FactorizedSRMetric extends BaseSRMetric {
    private TIntObjectMap<float[]> vectors;
    private Factorizer factorizer;
    private SRMetric baseMetric;

    public FactorizedSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig) {
        super(name, language, dao, disambig);
    }

    @Override
    public SRConfig getConfig() {
        return new SRConfig();
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        float v1[] = vectors.get(pageId1);
        float v2[] = vectors.get(pageId2);

        if (v1 == null || v2 == null) {
            return new SRResult(Double.NaN);
        } else {
            return new SRResult(simFn(v1, v2));   // cosine sim or dot?
        }
    }

    @Override
    public SRResultList mostSimilar(final int pageId, int maxResults, final TIntSet validIds) throws DaoException {
        final float [] v1 = vectors.get(pageId);
        if (v1 == null) {
            return null;
        }
        final Leaderboard top = new Leaderboard(maxResults);
        vectors.forEachEntry(new TIntObjectProcedure<float[]>() {
            @Override
            public boolean execute(int pageId2, float[] v2) {
                if (validIds == null || validIds.contains(pageId2)) {
                    top.tallyScore(pageId, simFn(v1, v2));
                }
                return true;
            }
        });
        return top.getTop();
    }

    @Override
    public void write() throws IOException {
        super.write();
        WpIOUtils.writeObjectToFile(new File(getDataDir(), "vectors.bin"), vectors);
    }


    @Override
    public void read() throws IOException{
        super.read();
        File f = new File(getDataDir(), "vectors.bin");
        if (f.isFile()) {
            vectors = (TIntObjectMap<float[]>) WpIOUtils.readObjectFromFile(f);
        }
    }


    private double simFn(float v1[], float v2[]) {
        return WbMathUtils.dot(v1, v2);
    }
}
