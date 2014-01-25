package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.matrix.MatrixLocalLinkDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.QueryBuilder;
import org.wikapidia.lucene.WikapidiaScoreDoc;
import org.wikapidia.lucene.WpIdFilter;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates a sparse vector containing a "1" for each inbound or outbound link
 * for a page.
 *
 * @author Shilad Sen
 */
public class MilneWittenGenerator implements VectorGenerator {

    private static final Logger LOG = Logger.getLogger(MilneWittenGenerator.class.getName());
    private boolean outLinks;
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final Language language;

    public MilneWittenGenerator(Language language, LocalLinkDao linkDao, LocalPageDao pageDao, boolean outLinks) {
        this.language = language;
        this.linkDao = linkDao;
        this.outLinks = outLinks;
        this.pageDao = pageDao;
    }


    @Override
    public TIntFloatMap getVector(int pageId) throws DaoException {
        TIntFloatMap vector = new TIntFloatHashMap();
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
                lb.insert(id, vector1.get(id) * vector2.get(id));
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

    public static class Provider extends org.wikapidia.conf.Provider<VectorGenerator> {
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
            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            return new MilneWittenGenerator(
                        language,
                    getConfigurator().get(LocalLinkDao.class),
                    getConfigurator().get(LocalPageDao.class),
                        config.getBoolean("outLinks")
                    );
        }
    }
}
