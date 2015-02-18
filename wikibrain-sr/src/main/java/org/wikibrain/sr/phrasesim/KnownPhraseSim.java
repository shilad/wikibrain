package org.wikibrain.sr.phrasesim;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;
import gnu.trove.procedure.TLongFloatProcedure;
import gnu.trove.set.TIntSet;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.normalize.IdentityNormalizer;
import org.wikibrain.sr.normalize.Normalizer;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
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
    private final Language language;
    private final Normalizer scoreNormalizer = new IdentityNormalizer();
    private final File dir;

    // Regular index
    private ConcurrentHashMap<String, KnownPhrase> byPhrase;
    private ConcurrentHashMap<Integer, KnownPhrase> byId;

    // Keeps an inverted index for fast mostSimilar performance
    private ConcurrentHashMap<Long, TIntFloatMap> invertedIndex = new ConcurrentHashMap<Long, TIntFloatMap>();

    // Caches the full cosimilarity matrix.
    private CosimilarityMatrix cosim = new CosimilarityMatrix();

    private DB phraseDb;

    public KnownPhraseSim(Language language, PhraseCreator creator, File dir, StringNormalizer normalizer) throws IOException {
        this.language = language;
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
        this.dir = dir;
        this.readPhrases();
        this.readCosimilarity();
    }

    private void readCosimilarity() throws IOException {
        File f = new File(dir, "cosimilarity.bin");
        try {
            this.cosim = (CosimilarityMatrix) WpIOUtils.readObjectFromFile(f);
        } catch (Exception e) {
            LOGGER.info("Reading cosim file " + f + " failed... rebuilding it from scratch");
            cosim = new CosimilarityMatrix();
        }
        final TIntSet built = cosim.getCompleted();
        ParallelForEach.loop(byId.values(), new Procedure<KnownPhrase>() {
            @Override
            public void call(KnownPhrase p) throws Exception {
                if (!built.contains(p.getId())) {
                    SRResultList neighbors = indexedMostSimilar(p.getVector(), byId.size(), null);
                    cosim.update(p.getId(), neighbors);
                }
            }
        });
    }

    public void writeCosimilaritySnapshot() throws IOException {
        WpIOUtils.writeObjectToFile(new File(dir, "cosimilarity.bin"), cosim);
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
            PhraseVector v = val.getVector();
            for (int i = 0; i < v.ids.length; i++) {
                long featureId = v.ids[i];
                float featureVal = v.vals[i];
                invertedIndex.putIfAbsent(featureId, new TIntFloatHashMap());
                TIntFloatMap index = invertedIndex.get(featureId);
                synchronized (index) {
                    invertedIndex.get(featureId).put(val.getId(), featureVal);
                }
            }
        }
    }

    public void addPhrase(String phrase, final int id) {
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
            vector.forEachEntry(new TLongFloatProcedure() {
                @Override
                public boolean execute(long k, float v) {
                    invertedIndex.putIfAbsent(k, new TIntFloatHashMap());
                    TIntFloatMap index = invertedIndex.get(k);
                    synchronized (index) {
                        invertedIndex.get(k).put(id, v);
                    }
                    return true;
                }
            });
            if (cosim != null) {
                SRResultList neighbors = indexedMostSimilar(ifAbsent.getVector(), byId.size(), null);
                cosim.update(id, neighbors);
            }
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
        return normalizer.normalize(language, phrase);
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
        if (cosim != null) {
            return cosim.mostSimilar(id, maxResults, candidateIds);
        }
        PhraseVector v1 = p.getVector();
        if (candidateIds != null && candidateIds.size() < 10) {
            return mostSimilar(v1, maxResults, candidateIds);
        } else {
            return mostSimilar(v1, maxResults, candidateIds);
//            return indexedMostSimilar(v1, maxResults, candidateIds);
        }
    }

    private SRResultList indexedMostSimilar(PhraseVector v1, int maxResults, TIntSet candidateIds) {
        final TIntDoubleHashMap dots = new TIntDoubleHashMap(maxResults * 5);

        for (int i = 0; i < v1.ids.length; i++) {
            long featureId = v1.ids[i];
            final float featureVal = v1.vals[i];
            TIntFloatMap index = invertedIndex.get(featureId);
            if (index == null) continue;
            synchronized (index) {
                index.forEachEntry(new TIntFloatProcedure() {
                    @Override
                    public boolean execute(int id, float val) {
                        dots.adjustOrPutValue(id, val * featureVal, val * featureVal);
                        return true;
                    }
                });
            }
        }

        final Leaderboard leaderboard = new Leaderboard(maxResults);
        double l1 = v1.norm2();
        int keys[] = dots.keys();
        for (int i = 0; i < keys.length; i++) {
            int id = keys[i];
            double l2 = byId.get(id).getVector().norm2();
            double dot = dots.get(id);
            double sim = dot / (l1 * l2);
            leaderboard.tallyScore(id, sim);
        }

        return leaderboard.getTop();
    }

    private SRResultList mostSimilar(PhraseVector v1, int maxResults, TIntSet candidateIds) {
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
        TIntList rowIds = new TIntArrayList();
        for (String s : rows) {
            rowIds.add(getId(s));
        }
        TIntList colIds = new TIntArrayList();
        for (String s : columns) {
            colIds.add(getId(s));
        }
        return cosimilarity(rowIds, colIds);
    }

    public double[][] cosimilarity(TIntList rows, TIntList columns) {
        double cosims[][] = new double[rows.size()][columns.size()];
        if (cosim != null) {
            return cosim.cosimilarity(rows, columns);
        }
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
