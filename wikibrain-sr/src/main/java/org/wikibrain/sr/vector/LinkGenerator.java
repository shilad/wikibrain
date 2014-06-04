package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Generates a sparse vector containing a "1" for each inbound or outbound link
 * for a page.
 *
 * @author Shilad Sen
 */
public class LinkGenerator implements VectorGenerator {

    private static final Logger LOG = Logger.getLogger(LinkGenerator.class.getName());
    private boolean outLinks;
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final Language language;

    public LinkGenerator(Language language, LocalLinkDao linkDao, LocalPageDao pageDao, boolean outLinks) {
        this.language = language;
        this.linkDao = linkDao;
        this.outLinks = outLinks;
        this.pageDao = pageDao;
    }


    @Override
    public TIntFloatMap getVector(int pageId) throws DaoException {
        TIntFloatMap vector = new TIntFloatHashMap(100);
        if (pageId <= 0) {
            throw new IllegalArgumentException("Invalid page id: " + pageId);
        }
        for (LocalLink link : linkDao.getLinks(language, pageId, outLinks)) {
            int columnId = outLinks ? link.getDestId() : link.getSourceId();
            if (columnId >= 0) {
                vector.put(columnId, 1);
            }
        }
        return vector;
    }

    @Override
    public TIntFloatMap getVector(String phrase) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Explanation> getExplanations(LocalPage page1, LocalPage page2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        Leaderboard lb = new Leaderboard(5);    // TODO: make 5 configurable
        for (int id : vector1.keys()) {
            if (vector2.containsKey(id)) {
                lb.tallyScore(id, vector1.get(id) * vector2.get(id));
            }
        }
        SRResultList top = lb.getTop();
        if (top.numDocs() == 0) {
            return Arrays.asList(new Explanation("? and ? share no links", page1, page2));
        }
        top.sortDescending();

        List<Explanation> explanations = new ArrayList<Explanation>();
        for (int i = 0; i < top.numDocs(); i++) {
            LocalPage p = pageDao.getById(language, top.getId(i));
            if (p == null) {
                continue;
            }
            if (outLinks) {
                explanations.add(new Explanation("Both ? and ? link to ?", page1, page2, p));
            } else {
                explanations.add(new Explanation("? links to both ? and ?", p, page1, page2));
            }
        }
        return explanations;
    }

    public static class Provider extends org.wikibrain.conf.Provider<VectorGenerator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return VectorGenerator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.generator";
        }

        @Override
        public VectorGenerator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("links")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            return new LinkGenerator(
                        language,
                    getConfigurator().get(LocalLinkDao.class),
                    getConfigurator().get(LocalPageDao.class),
                        config.getBoolean("outLinks")
                    );
        }
    }
}
