package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.Leaderboard;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generates an
 *
 * @author Shilad Sen
 */
public class MostSimilarConceptsGenerator implements VectorGenerator {

    private static final Logger LOG = Logger.getLogger(MostSimilarConceptsGenerator.class.getName());

    private final Language language;
    private final LocalPageDao pageDao;
    private final MonolingualSRMetric baseMetric;
    private final int numConcepts;
    private TIntSet conceptIds = null;

    public MostSimilarConceptsGenerator(Language language, LocalPageDao pageDao, MonolingualSRMetric baseMetric, int numConcepts) {
        this.language = language;
        this.pageDao = pageDao;
        this.baseMetric = baseMetric;
        this.numConcepts = numConcepts;
    }


    @Override
    public TIntFloatMap getVector(int pageId) throws DaoException {
        SRResultList mostSimilar = baseMetric.mostSimilar(pageId, numConcepts, conceptIds);
        if (mostSimilar == null) {
            return null;
        } else {
            return mostSimilar.asTroveMap();
        }
    }

    @Override
    public TIntFloatMap getVector(String phrase) {
        throw new UnsupportedOperationException();
    }

    /*
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
    }   */


    public void setConcepts(File file) throws IOException {
        conceptIds = new TIntHashSet();
        if (!file.isFile()) {
            LOG.warning("concept path " + file + " not a file; defaulting to all concepts");
            return;
        }
        for (String wpId : FileUtils.readLines(file)) {
            conceptIds.add(Integer.valueOf(wpId));
        }
        LOG.warning("installed " + conceptIds.size() + " concepts for " + language);
    }

    @Override
    public List<Explanation> getExplanations(LocalPage page1, LocalPage page2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        Leaderboard lb = new Leaderboard(5);    // TODO: make 5 configurable
        for (int id : vector1.keys()) {
            if (vector2.containsKey(id)) {
                lb.insert(id, vector1.get(id) * vector2.get(id));
            }
        }
        SRResultList top = lb.getTop();
        if (top.numDocs() == 0) {
            return Arrays.asList(new Explanation("? and ? share no similar pages", page1, page2));
        }
        top.sortDescending();

        List<Explanation> explanations = new ArrayList<Explanation>();
        for (int i = 0; i < top.numDocs(); i++) {
            LocalPage p = pageDao.getById(language, top.getId(i));
            if (p != null) {
                explanations.add(new Explanation("Both ? and ? are similar to ?", page1, page2, p));
            }
        }
        return explanations;
    }

    public static class Provider extends org.wikapidia.conf.Provider<VectorGenerator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return VectorGenerator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.generator";
        }

        @Override
        public VectorGenerator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("mostsimilarconcepts")) {
                return null;
            }
            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            MonolingualSRMetric baseMetric = getConfigurator().get(
                    MonolingualSRMetric.class,
                    config.getString("basemetric"),
                    "language",
                    language.getLangCode());
            MostSimilarConceptsGenerator generator = new MostSimilarConceptsGenerator(
                    language,
                    getConfigurator().get(LocalPageDao.class),
                    baseMetric,
                    config.hasPath("numConcepts") ? config.getInt("numConcepts") : 500
            );
            if (config.hasPath("concepts")) {
                try {
                    generator.setConcepts(FileUtils.getFile(
                            config.getString("concepts"),
                            language.getLangCode() + ".txt"));
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            }
            return generator;
        }
    }
}
