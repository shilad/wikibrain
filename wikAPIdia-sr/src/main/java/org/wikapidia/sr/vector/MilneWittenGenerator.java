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
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.QueryBuilder;
import org.wikapidia.lucene.WikapidiaScoreDoc;
import org.wikapidia.lucene.WpIdFilter;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private final Language language;

    public MilneWittenGenerator(Language language, LocalLinkDao linkDao, boolean outLinks) {
        this.language = language;
        this.linkDao = linkDao;
        this.outLinks = outLinks;
    }


    @Override
    public TIntFloatMap getVector(int pageId) throws DaoException {
        TIntFloatMap vector = new TIntFloatHashMap();
        for (LocalLink link : linkDao.getLinks(language, pageId, outLinks)) {
            int columnId = outLinks ? link.getDestId() : link.getSourceId();
            vector.put(columnId, 1);
        }
        return vector;
    }

    @Override
    public TIntFloatMap getVector(String phrase) {
        throw new UnsupportedOperationException();
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
                        config.getBoolean("outLinks")
                    );
        }
    }
}
