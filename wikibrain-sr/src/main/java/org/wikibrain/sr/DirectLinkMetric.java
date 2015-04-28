package org.wikibrain.sr;

import com.typesafe.config.Config;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a sparse vector containing both inbound and outbound links
 * for a page.
 *
 * @author Shilad Sen
 */
public class DirectLinkMetric extends BaseSRMetric {

    private static final Logger LOG = LoggerFactory.getLogger(DirectLinkMetric.class);
    private final LocalLinkDao linkDao;

    public DirectLinkMetric(String name, Language language, LocalPageDao dao, LocalLinkDao linkDao, Disambiguator dab) {
        super(name, language, dao,dab);
        this.linkDao = linkDao;
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
        int count = 0;
        int links1[] = getLinks(pageId1, true);
        int links2[] = getLinks(pageId2, true);
        count += hasLink(links1, pageId2);
        count += hasLink(links2, pageId1);
        return new SRResult(normalize(1.0 * count / 2.0));
    }

    @Override
    public double[][] cosimilarity(int rowIds[], int columnIds[]) throws DaoException {
        int [][] rowLinks = new int[rowIds.length][];
        int [][] colLinks = new int[columnIds.length][];
        for (int i = 0; i < rowIds.length; i++) rowLinks[i] = getLinks(rowIds[i], true);
        for (int i = 0; i < columnIds.length; i++) colLinks[i] = getLinks(columnIds[i], true);
        double result[][] = new double[rowIds.length][columnIds.length];
        for (int i = 0; i < rowIds.length; i++) {
            for (int j = 0; j < columnIds.length; j++) {
                int has1 = hasLink(rowLinks[i], columnIds[j]);
                int has2 = hasLink(colLinks[j], rowIds[i]);
                result[i][j] = normalize(0.5 * has1 + 0.5 * has2);
            }
        }
        return result;
    }


    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        TIntIntMap scores = new TIntIntHashMap();
        for (int id : getLinks(pageId, true)) {
            if (validIds == null || validIds.contains(id)) scores.adjustOrPutValue(id, 1, 1);
        }
        for (int id : getLinks(pageId, false)) {
            if (validIds == null || validIds.contains(id)) scores.adjustOrPutValue(id, 1, 1);
        }
        Leaderboard leaderboard = new Leaderboard(maxResults);
        for (int id : scores.keys()) {
            leaderboard.tallyScore(id, scores.get(id) / 2.0);
        }
        return normalize(leaderboard.getTop());
    }

    private int hasLink(int [] links, int targetId) {
        return (Arrays.binarySearch(links, targetId) >= 0) ? 1 : 0;
    }

    private int[] getLinks(int pageId1, boolean outLinks) throws DaoException {
        TIntList result = new TIntArrayList();
        for (LocalLink ll : linkDao.getLinks(getLanguage(), pageId1, outLinks)) {
            result.add(ll.getLocalId());
        }
        result.sort();
        return result.toArray();
    }


    public static class Provider extends org.wikibrain.conf.Provider<SRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("directlink")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            LocalLinkDao linkDao = getConfigurator().get(LocalLinkDao.class);
            Disambiguator dab = getConfigurator().get(Disambiguator.class, config.getString("disambiguator"), "language", language.getLangCode());
            DirectLinkMetric mw = new DirectLinkMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class),
                    linkDao,
                    dab
            );
            configureBase(getConfigurator(), mw, config);
            return mw;
        }
    }
}
