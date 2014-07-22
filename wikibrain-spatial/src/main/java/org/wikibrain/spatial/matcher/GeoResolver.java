package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.*;

/**
 * @author Shilad Sen
 */
public class GeoResolver {
    private final Env env;
    private final Config config;
    private final List<String> titleFields;
    private final Disambiguator disambig;
    private final List<String> contextFields;
    private final Language language;
    private final LocalPageDao pageDao;

    private List<AbstractMatchScorer> scorers;

    public GeoResolver(Env env, Config config) throws ConfigurationException {
        this.env = env;
        this.config = config;
        this.titleFields = config.getStringList("titles");
        this.contextFields = config.getStringList("context");
        this.language = env.getLanguages().getDefaultLanguage();
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.disambig = env.getConfigurator().get(Disambiguator.class, config.getString("dab"), "language", language.getLangCode());

        if (this.language != Language.EN && this.language != Language.SIMPLE) {
            throw new IllegalArgumentException();
        }

        initScorers();
    }

    private void initScorers() throws ConfigurationException {
        this.scorers = new ArrayList<AbstractMatchScorer>();
        for (Config scorerConfig : config.getConfigList("scorers")) {
            String type = scorerConfig.getString("type");
            if (type.equals("instanceOf")) {
                scorers.add(new InstanceOfMatchScorer(env, scorerConfig));
            } else if (type.equals("wikidataValue")) {
                scorers.add(new WikidataValueScorer(env, scorerConfig));
            } else if (type.equals("contains")) {
                scorers.add(new ContainsPointScorer(env, scorerConfig));
            } else {
                throw new ConfigurationException("Unknown score type: " + type);
            }
        }
    }

    public LinkedHashMap<LocalPage, Double> resolve(Map<String, String> row, Geometry geometry, int n) throws DaoException {

        List<String> titles = new ArrayList<String>();
        for (String field : titleFields) {
            for (String t : row.get(field).split("\\|")) {
                if (!titles.contains(t)) {
                    titles.add(t);
                }
            }
        }

        Set<LocalString> context = new HashSet<LocalString>();
        for (String field : contextFields) {
            if (row.get(field) != null) {
                context.add(new LocalString(language, row.get(field)));
            }
        }

        Map<LocalId, Double> scores = new HashMap<LocalId, Double>();
        for (int i = 0; i < titles.size(); i++) {
            double weight = 1.0 * Math.pow(0.7, i);
            LinkedHashMap<LocalId, Float> result = disambig.disambiguate(new LocalString(language, titles.get(i)), context);
            if (result == null) {
                continue;
            }
            for (LocalId lid : result.keySet()) {
                if (!scores.containsKey(lid)) {
                    scores.put(lid, 0.0);
                }
                scores.put(lid, scores.get(lid) + weight * result.get(lid));
            }
        }

        for (AbstractMatchScorer scorer : scorers) {
            for (LocalId id : scores.keySet()) {
                scores.put(id, scores.get(id) + scorer.getWeight() * scorer.score(id, row, geometry));
            }
        }

        LinkedHashMap<LocalPage, Double> result = new LinkedHashMap<LocalPage, Double>();
        for (LocalId id : WpCollectionUtils.sortMapKeys(scores, true)) {
            result.put(pageDao.getById(id), scores.get(id));
            if (result.size() >= n) {
                break;
            }
        }

        return result;
    }
}
