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
 * @author Shilad Sen
 */
public class PhraseVectorCreator {
    private final LuceneSearcher searcher;
    private Language language;
    private VectorBasedMonoSRMetric metric;
    private Disambiguator disambig;
    private VectorGenerator generator;

    private double dabWeight = 1.0;

    private double srWeight = 0.5;
    private int numSrCands = 0;
    private int numPerSrCand = 0;

    private double textWeight = 0.5;
    private int numTextCands = 0;

    private int numUsedCands = 5;

    public PhraseVectorCreator(LuceneSearcher searcher) {
        this.searcher = searcher;
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
        for (Map.Entry<LocalId, Double> entry : dabCandidates.entrySet()) {
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
        for (int i = 0; i < numUsedCands && i < ids.length; i++) {
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
                    expanded.put(new LocalId(language, sr.getId(j)), sr.getScore(i) * candidates.get(id1));
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
            return new PhraseVectorCreator(searcher);
        }
    }
}
