package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.wikidata.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class WikidataValueScorer extends AbstractMatchScorer {
    private static final Logger LOG = Logger.getLogger(WikidataValueScorer.class.getName());

    private WikidataDao wikidataDao;

    private int propertyId;
    private String columnName;

    public WikidataValueScorer(Env env, Config conf) throws ConfigurationException {
        super(env, conf);
        try {
            wikidataDao = env.getConfigurator().get(WikidataDao.class);
            columnName = conf.getString("column");
            Language lang = env.getLanguages().getBestAvailableEnglishLang(false);
            WikidataEntity prop = wikidataDao.getProperty(lang, conf.getString("property"));
            if (prop == null) {
                throw new IllegalArgumentException("Couldn't find property with name " + conf.getString("property"));
            }
            propertyId = prop.getId();
        } catch (WikiBrainException e) {
            throw new ConfigurationException(e);
        } catch (DaoException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public double score(LocalId candidate, Map<String, String> row) throws DaoException {
        if (!row.containsKey(columnName)) {
            throw new IllegalArgumentException("No column with name " + columnName + " in dbx");
        }
        String rowValue = row.get(columnName).toString();
        for (WikidataStatement st : wikidataDao.getStatements(candidate.asLocalPage())) {
            if (st.getProperty() == null || st.getProperty().getId() != propertyId) {
                continue;
            }

            String candidateVal = null;
            if (st.getValue().getType() == WikidataValue.Type.ITEM) {
                candidateVal = String.valueOf(st.getValue().getItemValue());
            } else if (st.getValue().getType() == WikidataValue.Type.INT) {
                candidateVal = String.valueOf(st.getValue().getIntValue());
            } else if (st.getValue().getType() == WikidataValue.Type.STRING) {
                candidateVal = st.getValue().getStringValue();
            } else {
                LOG.warning("Unexpected type for property " + st.getProperty() + ": " + st.getValue());
            }
            if (candidateVal != null && candidateVal.equalsIgnoreCase(rowValue)) {
                return 1.0;
            }
        }
        return 0.0;
    }
}
