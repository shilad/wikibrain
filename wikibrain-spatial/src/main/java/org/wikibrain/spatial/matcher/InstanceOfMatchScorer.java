package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataStatement;
import org.wikibrain.wikidata.WikidataValue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class InstanceOfMatchScorer extends AbstractMatchScorer {
    private WikidataDao wikidataDao;
    private Set<String> instanceOfKeywords;

    private static final int INSTANCE_OF_PROPERTY = 31;
    private final ConcurrentHashMap<Integer, String> propertyNames = new ConcurrentHashMap<Integer, String>();

    public InstanceOfMatchScorer(Env env, Config conf) throws ConfigurationException {
        super(env, conf);

        wikidataDao = env.getConfigurator().get(WikidataDao.class);
        String text = null;
        try {
            text = WpIOUtils.resourceToString(conf.getString("file"));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown resource: " + conf.getString("file"));
        }
        instanceOfKeywords = new HashSet<String>();
        for (String line : text.split("\n")) {
            instanceOfKeywords.add(line.toLowerCase().trim());
        }

    }

    @Override
    public double score(LocalId candidate, Map<String, String> row, Geometry geometry) throws DaoException {
        for (WikidataStatement st : wikidataDao.getStatements(candidate.asLocalPage())) {
            if (st.getProperty() == null || st.getProperty().getId() != INSTANCE_OF_PROPERTY) {
                continue;
            }

            if (st.getValue().getType() == WikidataValue.Type.ITEM) {
                String name = getPropertyName(st.getValue().getIntValue());
                if (name != null && instanceOfKeywords.contains(name.toLowerCase())) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }

    private String getPropertyName(int id) throws DaoException {
        String name = propertyNames.get(id);
        if (name != null && name.equals("__NULL__")) {
            return null;
        } else if (name != null) {
            return name;
        }
        WikidataEntity entity = wikidataDao.getItem(id);
        if (entity == null) {
            name = null;
        } else if (entity.getLabels().containsKey(Language.EN)) {
            name = entity.getLabels().get(Language.EN);
        } else if (entity.getLabels().containsKey(Language.SIMPLE)) {
            name = entity.getLabels().get(Language.SIMPLE);
        } else {
            name = null;
        }
        propertyNames.put(id, name == null ? "__NULL__"  : name);
        return name;
    }
}
