package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.TextFieldElements;
import org.wikapidia.lucene.WikapidiaScoreDoc;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.utils.WpCollectionUtils;

import java.util.*;

/**
 * Config looks like:
 *
 * {
 *     weights: {
 *         dab : 1.0
 *         sr : 1.0
 *         text : 1.0
 *     }
 *
 *      numCandidates : {
 *           sr : 10
 *           perSr : 2
 *           text : 50
 *           used : 20
 *     }
 * }
 *
 * A detailed description appears in the reference.conf
 *
 * @author Shilad Sen
 */
public class PhraseVectorCreator {
    private final LuceneSearcher searcher;
    private Language language;
    private VectorBasedMonoSRMetric metric;
    private Disambiguator disambig;
    private VectorGenerator generator;

    private double dabWeight = 1.0;
    private int numDabCands = 1;

    private double srWeight = 1.0;
    private int numSrCands = 0;
    private int numPerSrCand = 0;

    private double textWeight = 0.4;
    private int numTextCands = 50;

    private int numUsedCands = 20;

    public PhraseVectorCreator(LuceneSearcher searcher) {
        this.searcher = searcher;
    }

    public void setDabWeight(double dabWeight) {
        this.dabWeight = dabWeight;
    }

    public void setSrWeight(double srWeight) {
        this.srWeight = srWeight;
    }

    public void setNumSrCands(int numSrCands) {
        this.numSrCands = numSrCands;
    }

    public void setNumPerSrCand(int numPerSrCand) {
        this.numPerSrCand = numPerSrCand;
    }

    public void setTextWeight(double textWeight) {
        this.textWeight = textWeight;
    }

    public void setNumTextCands(int numTextCands) {
        this.numTextCands = numTextCands;
    }

    public void setNumUsedCands(int numUsedCands) {
        this.numUsedCands = numUsedCands;
    }

    public void setNumDabCands(int numDabCands) {
        this.numDabCands = numDabCands;
    }

    /**
     * Set metric must be called before this component can be used.
     * @param metric
     */
    public void setMetric(VectorBasedMonoSRMetric metric) {
        this.metric = metric;
        this.language = metric.getLanguage();
        this.disambig = metric.getDisambiguator();
        this.generator = metric.getGenerator();
    }

    public TIntFloatMap[] getPhraseVectors(String ... phrases) throws DaoException {
        List<LocalString> local = new ArrayList<LocalString>();
        for (String p : phrases) {
            local.add(new LocalString(language, p));
        }

        List<LinkedHashMap<LocalId, Double>> candidates = disambig.disambiguate(local, null);
        if (candidates.size() != phrases.length) throw new IllegalStateException();

        TIntFloatMap results[] = new TIntFloatMap[phrases.length];
        for (int i = 0; i < phrases.length; i++) {
            results[i] = getPhraseVector(phrases[i], candidates.get(i));
        }
        return results;
    }

    public TIntFloatMap getPhraseVector(String phrase) throws DaoException {
        LocalString ls = new LocalString(language, phrase);
        LinkedHashMap<LocalId, Double> candidates = disambig.disambiguate(ls, null);
        return getPhraseVector(phrase, candidates);
    }

    private TIntFloatMap getPhraseVector(String phrase, LinkedHashMap<LocalId, Double> dabCandidates) throws DaoException {
        if (dabCandidates == null || dabCandidates.isEmpty()) {
            return null;
        }
        LinkedHashMap<LocalId, Double> textCandidates = resolveTextual(phrase, numTextCands);
        LinkedHashMap<LocalId, Double> srCandidates = expandSR(phrase, dabCandidates, numSrCands, numPerSrCand);

//        StringBuffer buff = new StringBuffer("for phrase " + phrase + "\n");
        TIntDoubleMap merged = new TIntDoubleHashMap();
        double total = 0.0;
        int i = 0;
        for (Map.Entry<LocalId, Double> entry : dabCandidates.entrySet()) {
            if (i++ > numDabCands) { break; }
//            buff.append("\tdab: " + getTitle(entry.getKey()) + ": " + entry.getValue() + " * 1.0\n");
            double v = entry.getValue() * dabWeight;
            merged.adjustOrPutValue(entry.getKey().getId(), v, v);
            total += v;
        }
        for (Map.Entry<LocalId, Double> entry : textCandidates.entrySet()) {
//            buff.append("\ttext: " + getTitle(entry.getKey()) + ": " + entry.getValue() + " * 1.0\n");
            double v = entry.getValue() * textWeight;
            merged.adjustOrPutValue(entry.getKey().getId(), v, v);
            total += v;
        }
        for (Map.Entry<LocalId, Double> entry : srCandidates.entrySet()) {
//            buff.append("\tsr: " + getTitle(entry.getKey()) + ": " + entry.getValue() + " * 1.0\n");
            double v = entry.getValue() * srWeight;
            merged.adjustOrPutValue(entry.getKey().getId(), v, v);
            total += v;
        }
//        System.out.println(buff.toString() + "\n\n\n");

        int ids[] = WpCollectionUtils.sortMapKeys(merged, true);
        TIntFloatMap vector = new TIntFloatHashMap();
        for (i = 0; i < numUsedCands && i < ids.length; i++) {
            TIntFloatMap candidateVector = generator.getVector(ids[i]);
            if (candidateVector != null) {
                for (int id : candidateVector.keys()) {
                    double w = Math.sqrt(merged.get(ids[i]) / total);
                    double v = candidateVector.get(id);
                    vector.adjustOrPutValue(id, (float)(w * v), (float)(w * v));
                }
            }
        }
        if (vector.isEmpty()) {
            return null;
        } else {
            return vector;
        }
    }

    private String getTitle(LocalId id) throws DaoException {
        return metric.getLocalPageDao().getById(language, id.getId()).getTitle().toString();
    }


    private LinkedHashMap<LocalId, Double> resolveTextual(String phrase, int n) {
        if (n == 0) {
            return new LinkedHashMap<LocalId, Double>();
        }
        WikapidiaScoreDoc results[] = searcher.getQueryBuilderByLanguage(language)
                                            .setPhraseQuery(new TextFieldElements().addPlainText(), phrase)
                                            .setNumHits(n*2)
                                            .search();
        double total = 0.0;
        for (WikapidiaScoreDoc doc : results) {
            total += doc.score;
        }
        LinkedHashMap<LocalId, Double> expanded = new LinkedHashMap<LocalId, Double>();
        for (int i = 0; i < n && i < results.length; i++) {
            expanded.put(new LocalId(language, results[i].wpId), results[i].score / total);
        }
        return expanded;
    }

    /**
     * Expands a set of disambiguation candidates to include semantically related entities.
     * @param phrase
     * @param candidates
     * @param numCands
     * @param numPerCand
     * @return
     * @throws DaoException
     */
    private LinkedHashMap<LocalId, Double> expandSR(String phrase, LinkedHashMap<LocalId, Double> candidates, int numCands, int numPerCand) throws DaoException {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (numCands == 0 || numPerCand == 0) {
            return new LinkedHashMap<LocalId, Double>();
        }
        LinkedHashMap<LocalId, Double> expanded = new LinkedHashMap<LocalId, Double>();
        int i = 0;
        for (LocalId id1 : candidates.keySet()) {
            SRResultList sr = metric.mostSimilar(id1.getId(), numCands * 2);
            if (sr != null && sr.numDocs() > 0) {
                for (int j = 0; j < numPerCand && j < sr.numDocs(); j++) {
                    expanded.put(new LocalId(language, sr.getId(j)), sr.getScore(j) * candidates.get(id1));
                }
                if (i++ >= numCands) {
                    break;
                }
            }
        }
        return expanded;
    }

    public static class Provider extends org.wikapidia.conf.Provider<PhraseVectorCreator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PhraseVectorCreator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.phraseVectorCreator";
        }

        @Override
        public PhraseVectorCreator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            LuceneSearcher searcher = getConfigurator().get(LuceneSearcher.class, config.getString("lucene"));
            PhraseVectorCreator creator = new PhraseVectorCreator(searcher);
            if (config.hasPath("weights.dab")) {
                creator.setDabWeight(config.getDouble("weights.dab"));
            }
            if (config.hasPath("weights.sr")) {
                creator.setSrWeight(config.getDouble("weights.sr"));
            }
            if (config.hasPath("weights.text")) {
                creator.setTextWeight(config.getDouble("weights.text"));
            }
            if (config.hasPath("numCandidates.used")) {
                creator.setNumUsedCands(config.getInt("numCandidates.used"));
            }
            if (config.hasPath("numCandidates.dab")) {
                creator.setNumDabCands(config.getInt("numCandidates.dab"));
            }
            if (config.hasPath("numCandidates.text")) {
                creator.setNumTextCands(config.getInt("numCandidates.text"));
            }
            if (config.hasPath("numCandidates.sr")) {
                creator.setNumSrCands(config.getInt("numCandidates.sr"));
            }
            if (config.hasPath("numCandidates.perSr")) {
                creator.setNumPerSrCand(config.getInt("numCandidates.perSr"));
            }
            return creator;
        }
    }
}
