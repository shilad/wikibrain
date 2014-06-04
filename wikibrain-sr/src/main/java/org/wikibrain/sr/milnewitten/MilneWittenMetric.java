package org.wikibrain.sr.milnewitten;

import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.BaseMonolingualSRMetric;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Generates a sparse vector containing both inbound and outbound links
 * for a page.
 *
 * @author Shilad Sen
 */
public class MilneWittenMetric extends BaseMonolingualSRMetric {

    private static final Logger LOG = Logger.getLogger(MilneWittenMetric.class.getName());
    private final MonolingualSRMetric inlink;
    private final MonolingualSRMetric outlink;

    public MilneWittenMetric(String name, Language language, LocalPageDao dao, MonolingualSRMetric inlink, MonolingualSRMetric outlink, Disambiguator dab) {
        super(name, language, dao,dab);
        this.inlink = inlink;
        this.outlink = outlink;
    }

    @Override
    public SRConfig getConfig() {
        SRConfig config = new SRConfig();
        config.maxScore = 1.1f;
        config.minScore = 0;
        return config;
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        SRResult r1 = inlink.similarity(pageId1, pageId2, explanations);
        SRResult r2 = outlink.similarity(pageId1, pageId2, explanations);
        if (!r1.isValid() || !r2.isValid()) {
            return new SRResult(Double.NaN);
        } else {
            return new SRResult(0.5 * r1.getScore() + 0.5 * r2.getScore());
        }
    }

    @Override
    public synchronized void trainSimilarity(Dataset dataset) throws DaoException {
        inlink.trainSimilarity(dataset);
        outlink.trainSimilarity(dataset);
        super.trainSimilarity(dataset);
    }


    @Override
    public synchronized void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds){
        inlink.trainMostSimilar(dataset, numResults, validIds);
        outlink.trainMostSimilar(dataset, numResults, validIds);
        super.trainMostSimilar(dataset, numResults, validIds);
    }

    @Override
    public void write() throws IOException {
        inlink.write();
        outlink.write();
        super.write();
    }

    @Override
    public void read() throws IOException {
        inlink.read();
        outlink.read();
        super.read();
    }


    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        return null;
    }


    public static class Provider extends org.wikibrain.conf.Provider<MonolingualSRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return MonolingualSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public MonolingualSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("milnewitten")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            MonolingualSRMetric inlink = getConfigurator().get(
                    MonolingualSRMetric.class, config.getString("inlink"),
                    "language", language.getLangCode());
            MonolingualSRMetric outlink = getConfigurator().get(
                    MonolingualSRMetric.class, config.getString("outlink"),
                    "language", language.getLangCode());
            return new MilneWittenMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class),
                    inlink,
                    outlink,
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator"))
                    );
        }
    }
}
