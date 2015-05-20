package org.wikibrain.sr.synrank;

import com.typesafe.config.Config;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.BaseSRMetric;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.disambig.Disambiguator;

import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the SynRank SR metric as described in
 *
 * G. Quercini, H. Samet Uncovering the spatial relatedness in Wikipedia.
 * In Y. Huang, M. Gertz, J. C. Krumm, J. Sankaranarayanan, and M. Schneider, editors,
 * Proceedings of SIGSPATIAL 2014.
 *
 * Soon to be linked at http://www.cs.umd.edu/~hjs/hjsyear.html
 *
 * @author Shilad Sen
 */
public class SynRank extends BaseSRMetric {
    private static final Logger LOG = LoggerFactory.getLogger(SynRank.class);

    private final LocalLinkDao linkDao;
    private final int numArticles;
    private final TIntHashSet dabs;

    public SynRank(String name, Language language, LocalPageDao pageDao, Disambiguator dab, LocalLinkDao linkDao, LocalCategoryMemberDao catDao) throws DaoException {
        super(name, language, pageDao, dab);
        this.linkDao = linkDao;
        LOG.info("calculating number of articles");
        this.numArticles = pageDao.getCount(DaoFilter.normalPageFilter(language));
        LOG.info("found " + this.numArticles + " articles");
        this.dabs = new TIntHashSet();

        // TODO: fix dabs!
        LocalPage dabCat = null;
        for (String title: Arrays.asList("Category:Disambiguation pages", "Category:Disambiguation")) {
            dabCat = pageDao.getByTitle(getLanguage(), NameSpace.CATEGORY, title);
            if (dabCat != null) break;
        }
        if (dabCat == null) throw new IllegalArgumentException();
        for (int id : catDao.getCategoryMemberIds(dabCat)) {
            dabs.add(id);
        }
        LOG.info("identified " + dabs.size() + " disambiguation pages");
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
        if (pageId1 == pageId2) {
            return new SRResult(1.0);
        }
        if (dabs.contains(pageId1) || dabs.contains(pageId2)) {
            return null;
        }

        TIntSet ids1 = getLinksTo(pageId1);
        TIntSet ids2 = getLinksTo(pageId2);
        TIntSet both = new TIntHashSet(ids1);

        both.retainAll(ids2);
        if (both.isEmpty()) {
            return new SRResult(0.0);
        }

        double pmi = 1.0 * numArticles * both.size() / (ids1.size() * ids2.size());
        double boost = Math.log10(both.size());
        double graphDistance = graphDistance(pageId1, pageId2);
//        String t1 = getLocalPageDao().getById(getLanguage(), pageId1).getTitle().toString();
//        String t2 = getLocalPageDao().getById(getLanguage(), pageId2).getTitle().toString();
//        System.err.println(String.format("Values for %s, %s, are %.4f, %.4f, %.4f\n", t1, t2, pmi, boost, graphDistance));

        return new SRResult(pmi * boost / graphDistance);
    }

    private TIntSet getLinksTo(int pageId) throws DaoException {
        TIntSet ids = new TIntHashSet();
        for (LocalLink ll : linkDao.get(new DaoFilter().setDestIds(pageId).setLanguages(getLanguage()))) {
            ids.add(ll.getSourceId());
        }
        return ids;
    }

    private TIntSet getLinksFrom(int pageId) throws DaoException {
        TIntSet ids = new TIntHashSet();
        for (LocalLink ll : linkDao.get(new DaoFilter().setSourceIds(pageId).setLanguages(getLanguage()))) {
            ids.add(ll.getDestId());
        }
        return ids;
    }

    private int graphDistance(int pageId1, int pageId2) throws DaoException {
        if (getLinksTo(pageId1).contains(pageId2) || getLinksFrom(pageId1).contains(pageId2)) {
            return 1;
        } else {
            return 2;
        }
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        throw new UnsupportedOperationException();
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
            if (!config.getString("type").equals("synrank")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            Disambiguator dab = getConfigurator().get(Disambiguator.class, config.getString("disambiguator"), "language", language.getLangCode());
            try {
                SynRank sr = new SynRank(
                        name,
                        language,
                        getConfigurator().get(LocalPageDao.class),
                        dab,
                        getConfigurator().get(LocalLinkDao.class),
                        getConfigurator().get(LocalCategoryMemberDao.class)
                    );
                configureBase(getConfigurator(), sr, config);
                return sr;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
