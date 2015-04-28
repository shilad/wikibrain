package org.wikibrain.sr.milnewitten;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.*;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a sparse vector containing both inbound and outbound links
 * for a page.
 *
 * @author Shilad Sen
 */
public class MilneWittenMetric extends BaseSRMetric {

    private static final Logger LOG = LoggerFactory.getLogger(MilneWittenMetric.class);
    private final SRMetric inlink;
    private final SRMetric outlink;
    private boolean trainSubmetrics =true;

    public MilneWittenMetric(String name, Language language, LocalPageDao dao, SRMetric inlink, SRMetric outlink, Disambiguator dab) {
        super(name, language, dao,dab);
        this.inlink = inlink;
        this.outlink = outlink;
    }

    @Override
    public SRConfig getConfig() {
        SRConfig config = new SRConfig();
        config.maxScore = 1.1f;
        config.minScore = 0;
        return config;
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        SRResult r1 = inlink.similarity(pageId1, pageId2, explanations);
        SRResult r2 = outlink.similarity(pageId1, pageId2, explanations);
        if (r1 == null || r2 == null || !r1.isValid() || !r2.isValid()) {
            return new SRResult(Double.NaN);
        } else {
            SRResult finalResult=new SRResult(0.5 * r1.getScore() + 0.5 * r2.getScore());
            if (explanations) {
                List<Explanation> explanationList = new ArrayList<Explanation>();
                explanationList.addAll(r1.getExplanations());
                explanationList.addAll(r2.getExplanations());
                finalResult.setExplanations(explanationList);
            }
            return normalize(finalResult);
        }
    }

    @Override
    public double[][] cosimilarity(int rowIds[], int columnIds[]) throws DaoException {
        double [][] cm1 = inlink.cosimilarity(rowIds, columnIds);
        double [][] cm2 = outlink.cosimilarity(rowIds, columnIds);
        for (int i = 0; i < rowIds.length; i++) {
            for (int j = 0; j < columnIds.length; j++) {
                double s1 = cm1[i][j];
                double s2 = cm2[i][j];
                if (Double.isNaN(s1) || Double.isNaN(s2) || Double.isInfinite(s1) || Double.isInfinite(s2)) {
                    cm1[i][j] = Double.NaN;
                } else {
                    cm1[i][j] = normalize(s1 * 0.5 + s2 * 0.5);
                }
            }
        }
        return cm1;
    }

    public void setTrainSubmetrics(boolean train){
        trainSubmetrics = train;
    }

    @Override
    public synchronized void trainSimilarity(Dataset dataset) throws DaoException {
        if(trainSubmetrics) {
            inlink.trainSimilarity(dataset);
            outlink.trainSimilarity(dataset);
        }
        super.trainSimilarity(dataset);
    }


    @Override
    public synchronized void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds){
        if(trainSubmetrics){
            inlink.trainMostSimilar(dataset, numResults, validIds);
            outlink.trainMostSimilar(dataset, numResults, validIds);
        }
        super.trainMostSimilar(dataset, numResults, validIds);
    }

    @Override
    public void write() throws IOException {
        inlink.write();
        outlink.write();
        super.write();
    }

    @Override
    public void read() throws IOException {
        inlink.read();
        outlink.read();
        super.read();
    }



    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        SRResultList l1 = inlink.mostSimilar(pageId, maxResults * 2, validIds);
        TIntDoubleMap scores = new TIntDoubleHashMap(maxResults * 4);

        TIntSet inList1 = new TIntHashSet();
        if (l1 != null) {
            for (int i = 0; i < l1.numDocs(); i++) {
                double s = l1.getScore(i);
                if (!Double.isInfinite(s) && !Double.isNaN(s)) {
                    scores.adjustOrPutValue(l1.getId(i), 0.5 * s, 0.5 * s);
                    inList1.add(l1.getId(i));
                }
            }
        }
        SRResultList l2 = outlink.mostSimilar(pageId, maxResults * 2, validIds);
        TIntSet inList2 = new TIntHashSet();
        if (l2 != null) {
            for (int i = 0; i < l2.numDocs(); i++) {
                double s = l2.getScore(i);
                if (!Double.isInfinite(s) && !Double.isNaN(s)) {
                    scores.adjustOrPutValue(l2.getId(i), 0.5 * s, 0.5 * s);
                    inList2.add(l2.getId(i));
                }
            }
        }

        double missingScore1 = (l1 == null) ? 0.0 : l1.getMissingScore();
        double missingScore2 = (l2 == null) ? 0.0 : l2.getMissingScore();

        for (int p1 : inList1.toArray()) {
            if (!inList2.contains(p1)) {
                scores.adjustValue(p1, 0.5 * missingScore2);
            }
        }
        for (int p2 : inList2.toArray()) {
            if (!inList1.contains(p2)) {
                scores.adjustValue(p2, 0.5 * missingScore1);
            }
        }

        Leaderboard leaderboard = new Leaderboard(maxResults);
        for (int id : scores.keys()) {
            leaderboard.tallyScore(id, scores.get(id));
        }
        return normalize(leaderboard.getTop());
    }


    public static class Provider extends org.wikibrain.conf.Provider<SRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("milnewitten")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            SRMetric inlink = getConfigurator().get(
                    SRMetric.class, config.getString("inlink"),
                    "language", language.getLangCode());
            SRMetric outlink = getConfigurator().get(
                    SRMetric.class, config.getString("outlink"),
                    "language", language.getLangCode());
            Disambiguator dab = getConfigurator().get(Disambiguator.class, config.getString("disambiguator"), "language", language.getLangCode());
            MilneWittenMetric mw = new MilneWittenMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class),
                    inlink,
                    outlink,
                    dab
            );
            configureBase(getConfigurator(), mw, config);
            return mw;
        }
    }
}
