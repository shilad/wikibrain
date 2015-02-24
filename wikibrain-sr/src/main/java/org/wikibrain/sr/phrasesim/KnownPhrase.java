package org.wikibrain.sr.phrasesim;

import java.io.Serializable;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class KnownPhrase implements Serializable {
    private final int id;
    private final String normalizedPhrase;
    private final Map<String, Integer> versions;
    private PhraseVector vector;

    public KnownPhrase(int id, String phrase, String normalizedPhrase) {
        this.id = id;
        this.normalizedPhrase = normalizedPhrase;
        this.versions = new HashMap<String, Integer>();
        this.versions.put(phrase, 1);
    }

    public KnownPhrase(KnownPhrase ks) {
        this.id = ks.id;
        this.normalizedPhrase = ks.normalizedPhrase;
        this.versions = new HashMap<String, Integer>(ks.versions);
        this.vector = ks.vector;
    }

    public void increment(String phrase) {
        synchronized (versions) {
            if (!versions.containsKey(phrase)) {
                versions.put(phrase, 1);
            } else {
                versions.put(phrase, versions.get(phrase) + 1);
            }
        }
    }

    public Set<String> getVersions() {
        return versions.keySet();
    }

    public void setVector(PhraseVector vector) {
        this.vector = vector;
    }

    public int getId() {
        return id;
    }

    public String getNormalizedPhrase() {
        return normalizedPhrase;
    }

    public String getCanonicalVersion() {
        synchronized (versions) {
            if (versions.isEmpty()) {
                throw new IllegalStateException();
            }
            String mostPopular = null;
            int mostPopularCount = 0;
            for (Map.Entry<String, Integer> entry : versions.entrySet()) {
                if (mostPopular == null || entry.getValue() > mostPopularCount) {
                    mostPopular = entry.getKey();
                    mostPopularCount = entry.getValue();
                }
            }
            return mostPopular;
        }
    }

    public PhraseVector getVector() {
        return vector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KnownPhrase that = (KnownPhrase) o;

        if (id != that.id) return false;
        if (!normalizedPhrase.equals(that.normalizedPhrase)) return false;
        if (!vector.equals(that.vector)) return false;
        if (!versions.equals(that.versions)) return false;

        return true;
    }
}
