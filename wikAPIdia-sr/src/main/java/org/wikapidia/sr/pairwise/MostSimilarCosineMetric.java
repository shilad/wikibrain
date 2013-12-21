package org.wikapidia.sr.pairwise;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.sr.BaseMonolingualSRMetric;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.SimUtils;

import java.io.IOException;
import java.util.*;

/**
 *@author Matt Lesicko
 **/
public class MostSimilarCosineMetric extends BaseMonolingualSRMetric {
    private final int MAX_RESULTS = 500;
    private MonolingualSRMetric baseMetric;

    public MostSimilarCosineMetric(Language language, Disambiguator disambiguator, LocalPageDao pageHelper, MonolingualSRMetric baseMetric){
        super(language, pageHelper, disambiguator);
        this.baseMetric=baseMetric;
    }

    @Override
    public String getName() {
        return "MostSimilarCosine";
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        List<LocalString> phrases = Arrays.asList(
                new LocalString(getLanguage(), phrase1),
                new LocalString(getLanguage(), phrase2));

        List<LinkedHashMap<LocalId,Double>> resolution = getDisambiguator().disambiguate(phrases, null);
        TIntDoubleMap vector1 = createWeightedVector(phrase1, expand(phrase1, resolution.get(0), 3, 3));
        TIntDoubleMap vector2 = createWeightedVector(phrase2, expand(phrase2, resolution.get(1), 3, 3));
        if (vector1 == null || vector2 == null) {
            return null;
        } else {
            return normalize(new SRResult(SimUtils.cosineSimilarity(vector1, vector2)));
        }
    }

    private String getTitle(int id) throws DaoException {
        return getLocalPageDao().getById(getLanguage(), id).getTitle().toString();
    }

    private LinkedHashMap<LocalId, Double> expand(String phrase, LinkedHashMap<LocalId, Double> candidates, int numCands, int numPerCand) throws DaoException {
        if (candidates.isEmpty()) {
            return null;
        }
        LinkedHashMap<LocalId, Double> expanded = new LinkedHashMap<LocalId, Double>();
        int i = 0;
        for (LocalId id1 : candidates.keySet()) {
            SRResultList sr = baseMetric.mostSimilar(id1.getId(), numCands*2);
            if (sr != null) {
                for (int j = 0; j < numPerCand; j++) {
                    expanded.put(new LocalId(getLanguage(), sr.getId(j)), sr.getScore(i) * candidates.get(id1));
                }
                if (i++ >= numCands) {
                    break;
                }
            }
        }
        System.out.println("for " + phrase + ": ");
        for (LocalId lid : expanded.keySet()) {
            System.out.println("\t" + getTitle(lid.getId()) + ": " + expanded.get(lid));
        }
        return expanded;
    }

    private TIntDoubleMap createWeightedVector(String phrase, LinkedHashMap<LocalId, Double> candidates) throws DaoException {
        TIntDoubleMap vector = new TIntDoubleHashMap();
        int j = 0;
        for (Map.Entry<LocalId, Double> entry : candidates.entrySet()) {
            SRResultList sr = baseMetric.mostSimilar(entry.getKey().getId(), MAX_RESULTS);
            if (sr != null) {
                for (int i = 0; i < sr.numDocs(); i++) {
                    double w = Math.sqrt(entry.getValue());
                    vector.adjustOrPutValue(sr.getId(i), w * sr.getScore(i), w * sr.getScore(i));
                }
                if (j++ >= 10) {
                    break;
                }
            }
        }
        if (vector.isEmpty()) {
            return null;
        } else {
            return vector;
        }
    }

    @Override
    public SRResult similarity(int page1, int page2, boolean explanations) throws DaoException {
        SRResultList mostSimilar1 = baseMetric.mostSimilar(page1,MAX_RESULTS);
        SRResultList mostSimilar2 = baseMetric.mostSimilar(page2,MAX_RESULTS);
        if (mostSimilar1 == null || mostSimilar2 == null) {
            return null;
        }
        double sim = SimUtils.cosineSimilarity(mostSimilar1.asTroveMap(), mostSimilar2.asTroveMap());
        return normalize(new SRResult(sim));
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        super.trainSimilarity(dataset);
//        baseMetric.trainMostSimilar(highDataset,MAX_RESULTS, null);
    }

    @Override
    public SRResultList mostSimilar(int page, int maxResults) throws DaoException {
        return mostSimilar(page, maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(int page, int maxResults, TIntSet validIds) throws DaoException {
        return baseMetric.mostSimilar(page, maxResults, validIds);
    }

    @Override
    public TIntDoubleMap getVector(int id) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path, int maxHits, TIntSet rowIds, TIntSet colIds) throws IOException, DaoException, WikapidiaException {
        return;
    }

    @Override
    public void readCosimilarity(String path) throws IOException {
        return;
    }

    public static class Provider extends org.wikapidia.conf.Provider<MonolingualSRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return MonolingualSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public MonolingualSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("mostsimilarcosine")) {
                return null;
            }

            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            MostSimilarCosineMetric sr = new MostSimilarCosineMetric(
                    language,
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(MonolingualSRMetric.class,config.getString("basemetric"), "language", language.getLangCode())
            );
            BaseMonolingualSRMetric.configureBase(getConfigurator(), sr, config);

            return sr;
        }

    }
}
