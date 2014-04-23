package org.wikibrain.core.lang;

/**
 * @author Ari Weiland
 */
public class UniversalId {
    private final int algorithmId;
    private final int id;

    public UniversalId(int algorithmId, int id) {
        this.algorithmId = algorithmId;
        this.id = id;
    }

    public int getAlgorithmId() {
        return algorithmId;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UniversalId universalId = (UniversalId) o;

        return id == universalId.id && algorithmId == universalId.algorithmId;
    }
}
