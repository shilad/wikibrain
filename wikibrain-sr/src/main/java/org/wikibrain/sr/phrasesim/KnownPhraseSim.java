package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.set.TIntSet;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class KnownPhraseSim {
    private static final Logger LOGGER = Logger.getLogger(KnownPhraseSim.class.getName());

    private final StringNormalizer normalizer;
    private final HTreeMap<Object, Object> db;
    private final PhraseCreator creator;
    private ConcurrentHashMap<String, KnownPhrase> byPhrase;
    private ConcurrentHashMap<Integer, KnownPhrase> byId;
    private DB phraseDb;

    public KnownPhraseSim(PhraseCreator creator, File dir, StringNormalizer normalizer) {
        this.creator = creator;
        this.normalizer = normalizer;
        this.phraseDb = DBMaker
                .newFileDB(new File(dir, "phrases.mapdb"))
                .mmapFileEnable()
                .transactionDisable()
                .asyncWriteEnable()
                .asyncWriteFlushDelay(100)
                .make();
        this.db = phraseDb.getHashMap("phrases");
        this.readPhrases();
    }

    private void readPhrases() {
        byId = new ConcurrentHashMap<Integer, KnownPhrase>();
        byPhrase = new ConcurrentHashMap<String, KnownPhrase>();
        for (Map.Entry entry : db.entrySet()) {
            String key = (String) entry.getKey();
            KnownPhrase val = (KnownPhrase) entry.getValue();
            if (!key.equals(val.getNormalizedPhrase())) {
                throw new IllegalStateException();
            }
            byId.put(val.getId(), val);
            byPhrase.put(val.getNormalizedPhrase(), val);
            for (String version : val.getVersions()) {
                byPhrase.put(version, val);
            }
        }
    }

    public void addPhrase(String phrase, int id) {
//        LOGGER.info("adding " + phrase);
        KnownPhrase ifAbsent = new KnownPhrase(id, phrase, normalize(phrase));
        KnownPhrase old = byPhrase.putIfAbsent(ifAbsent.getNormalizedPhrase(), ifAbsent);
        if (old == null) {
            TLongFloatMap vector = creator.getVector(phrase);
            if (vector == null) {
                return;
            }
            ifAbsent.setVector(new PhraseVector(vector));
            byId.put(id, ifAbsent);
            db.put(ifAbsent.getNormalizedPhrase(), new KnownPhrase(ifAbsent));
        } else {
            old.increment(phrase);
            db.put(ifAbsent.getNormalizedPhrase(), new KnownPhrase(old));
        }
    }

    public String getPhrase(int id) {
        if (byId.containsKey(id)) {
            return byId.get(id).getCanonicalVersion();
        }
        return null;
    }

    public Integer getId(String phrase) {
        KnownPhrase kp = byPhrase.get(normalize(phrase));
        if (kp == null) {
            return null;
        } else {
            return kp.getId();
        }
    }

    public String normalize(String phrase) {
        return normalizer.normalize(Language.EN, phrase);
    }

    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet candidateIds) {
        Integer id = getId(phrase);
        if (id == null) {
            return null;
        } else {
            return mostSimilar(id, maxResults, candidateIds);
        }
    }

    public SRResultList mostSimilar(String phrase, int maxResults) {
        return mostSimilar(phrase, maxResults, null);
    }

    public SRResultList mostSimilar(int id, int maxResults, TIntSet candidateIds) {
        KnownPhrase p = byId.get(id);
        if (p == null) {
            return null;
        }
        PhraseVector v1 = p.getVector();

        final Leaderboard top = new Leaderboard(maxResults);
        if (candidateIds != null) {
            for (int id2 : candidateIds.toArray()) {
                KnownPhrase p2 = byId.get(id2);
                if (p2 != null) {
                    top.tallyScore(id2, v1.cosineSim(p2.getVector()));
                }
            }
        } else {
            for (KnownPhrase p2 : byId.values()) {
                top.tallyScore(p2.getId(), v1.cosineSim(p2.getVector()));
            }
        }
        return top.getTop();
    }

    public SRResultList mostSimilar(int id, int maxResults) {
        return mostSimilar(id, maxResults, null);
    }

    public double[][] phraseCosimilarity(List<String> rows, List<String> columns) {
        List<Integer> rowIds = new ArrayList<Integer>();
        for (String s : rows) {
            rowIds.add(getId(s));
        }
        List<Integer> colIds = new ArrayList<Integer>();
        for (String s : columns) {
            colIds.add(getId(s));
        }
        return cosimilarity(rowIds, colIds);
    }

    public double[][] cosimilarity(List<Integer> rows, List<Integer> columns) {
        double cosims[][] = new double[rows.size()][columns.size()];
        List<PhraseVector> colVectors = new ArrayList<PhraseVector>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            KnownPhrase kp = byId.get(columns.get(i));
            colVectors.add(kp == null ? null : kp.getVector());
        }
        for (int i = 0; i < rows.size(); i++) {
            KnownPhrase kp = byId.get(columns.get(i));
            if (kp == null) {
                continue;   // leave sims as their default value of 0.0
            }
            PhraseVector v1 = kp.getVector();
            for (int j = 0; j < columns.size(); j++) {
                PhraseVector v2 = colVectors.get(j);
                if (v2 != null) {
                    cosims[i][j] = v1.cosineSim(v2);
                }
            }
        }
        return cosims;
    }
}
