package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import org.mapdb.HTreeMap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.StringNormalizer;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 * @author Shilad Sen
 */
public class KnownPhraseSim {
    private final StringNormalizer normalizer;
    private final HTreeMap<Object, Object> db;
    private final SimplePhraseCreator creator;
    private ConcurrentHashMap<String, KnownPhrase> byPhrase;
    private ConcurrentHashMap<Long, KnownPhrase> byId;
    private DB phraseDb;

    public KnownPhraseSim(SimplePhraseCreator creator, File dir, StringNormalizer normalizer) {
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
        byId = new ConcurrentHashMap<Long, KnownPhrase>();
        byPhrase = new ConcurrentHashMap<String, KnownPhrase>();
        for (Map.Entry entry : db.entrySet()) {
            String key = (String) entry.getKey();
            KnownPhrase val = (KnownPhrase) entry.getValue();
            if (entry.getKey().equals(val.getNormalizedPhrase())) {
                throw new IllegalStateException();
            }
            byId.put(val.getId(), val);
            byPhrase.put(val.getNormalizedPhrase(), val);
            for (String version : val.getVersions()) {
                byPhrase.put(version, val);
            }
        }
    }

    public void addPhrase(String phrase, long id) {
        KnownPhrase ifAbsent = new KnownPhrase(id, phrase, normalize(phrase));
        KnownPhrase old = byPhrase.putIfAbsent(ifAbsent.getNormalizedPhrase(), ifAbsent);
        if (old == null) {
            TIntFloatMap vector = creator.getVector(phrase);
            ifAbsent.setVector(new PhraseVector(vector));
            byId.put(id, ifAbsent);
            db.put(ifAbsent.getNormalizedPhrase(), new KnownPhrase(ifAbsent));
        } else {
            old.increment(phrase);
            db.put(ifAbsent.getNormalizedPhrase(), new KnownPhrase(old));
        }
    }

    public String getPhrase(long id) {
        if (byId.containsKey(id)) {
            return byId.get(id).getCanonicalVersion();
        }
        return null;
    }

    public Long getId(String phrase) {
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

    public LinkedHashMap<String, Double> mostSimilar(String phrase, int maxResults) {
        Long id = getId(phrase);
        if (id == null) {
            return null;
        } else {
            return mostSimilar(id, maxResults);
        }
    }

    public LinkedHashMap<String, Double> mostSimilar(long id, int maxResults) {
        return null;
    }

    public double[][] phraseCosimilarity(List<String> rows, List<String> columns) {
        return null;
    }

    public double[][] cosimilarity(List<Long> rows, List<Long> columns) {
        return null;
    }
}
