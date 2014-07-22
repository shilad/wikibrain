package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataStatement;

import java.util.Map;

/**
 * @author Shilad Sen
 */
public class ContainsPointScorer extends AbstractMatchScorer {
    private WikidataDao wikidataDao;

    private static final int COORDINATE_PROPERTY = 625;

    public ContainsPointScorer(Env env, Config conf) throws ConfigurationException {
        super(env, conf);
        wikidataDao = env.getConfigurator().get(WikidataDao.class);
    }

    @Override
    public double score(LocalId candidate, Map<String, String> row, Geometry geometry) throws DaoException {
        for (WikidataStatement st : wikidataDao.getStatements(candidate.asLocalPage())) {
            if (st.getProperty() == null || st.getProperty().getId() != COORDINATE_PROPERTY) {
                continue;
            }

            Geometry point = WikiBrainSpatialUtils.jsonToGeometry(st.getValue().getJsonValue().getAsJsonObject());
            if (point != null && geometry.contains(point)) {
                return 1.0;
            }
        }
        return 0.0;
    }
}
