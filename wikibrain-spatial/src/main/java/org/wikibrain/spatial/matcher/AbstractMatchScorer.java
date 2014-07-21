package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public abstract class AbstractMatchScorer {

    private final double weight;
    private final Env env;

    public AbstractMatchScorer(Env env, Config conf) {
        this.env = env;
        this.weight = conf.getDouble("weight");
    }

    public abstract double score(LocalId candidate, Map<String, String> row, Geometry geometry) throws DaoException;

    public double getWeight() {
        return weight;
    }

    public Env getEnv() {
        return env;
    }
}
