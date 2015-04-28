package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.function.TFloatFunction;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
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
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a sparse vector containing a "1" for each inbound or outbound link
 * for a page.
 *
 * @author Shilad Sen
 */
public class LinkGenerator implements SparseVectorGenerator {

    public static enum LinkType {
        IN,
        OUT,
        DIRECT
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinkGenerator.class);
    private boolean outLinks;
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final Language language;
    private final TIntIntMap linkCounts = new TIntIntHashMap();
    private boolean weightByPopularity = false;
    private boolean logTransform = false;
    private final int numPages;
    private TIntSet blackListSet;
    private final String blackListFilePath;

    public LinkGenerator(Language language, LocalLinkDao linkDao, LocalPageDao pageDao, boolean outLinks, String blackListFilePath) throws DaoException, FileNotFoundException {
        this.language = language;
        this.linkDao = linkDao;
        this.outLinks = outLinks;
        this.pageDao = pageDao;
        this.blackListFilePath = blackListFilePath;
        numPages = pageDao.getCount(
                new DaoFilter().setLanguages(language)
                        .setRedirect(false)
                        .setDisambig(false)
                        .setNameSpaces(NameSpace.ARTICLE)
        );
        createBlackListSet();
    }

    private void createBlackListSet() throws FileNotFoundException {
        blackListSet = new TIntHashSet();
        if(blackListFilePath == null || blackListFilePath.equals("")) {
            LOG.info("Skipping blacklist creation; no blacklist file specified.");
            return;
        }

        File file = new File(blackListFilePath);
        Scanner scanner = new Scanner(file);
        while(scanner.hasNext()){
            blackListSet.add(scanner.nextInt());
        }

        scanner.close();
    }


    @Override
    public TIntFloatMap getVector(int pageId) throws DaoException {
        TIntFloatMap vector = new TIntFloatHashMap(100);
        if (pageId <= 0) {
            throw new IllegalArgumentException("Invalid page id: " + pageId);
        }
        double norm2 = 0.0;
        for (LocalLink link : linkDao.getLinks(language, pageId, outLinks)) {

            int columnId = outLinks ? link.getDestId() : link.getSourceId();
            if (columnId < 0) {
                continue;
            }
            if(isBlacklisted(columnId)){
                continue;
            }
            double value = 1;
            if (weightByPopularity) {
                value = numPages / getNumLinks(columnId);
                if (logTransform) {
                    value = Math.log(value);
                }
            }
            vector.put(columnId, (float) value);
            norm2 += value * value;
        }
        final double n = norm2;

        vector.transformValues(new TFloatFunction() {
            @Override
            public float execute(float value) {
                return (float) (value / n);
            }
        });
        return vector;
    }

    private boolean isBlacklisted(int pageId) {
        return blackListSet.contains(pageId);
    }

    /**
     * If outLinks is true, returns the number of links to the specified destination.
     * Otherwise, returns number of links FROM the specified source.
     * @param wpId
     * @return
     * @throws DaoException
     */
    private int getNumLinks(int wpId) throws DaoException {
        synchronized (linkCounts) {
            if (linkCounts.containsKey(wpId)) {
                return linkCounts.get(wpId);
            }
        }
        int n;
        if (outLinks) {
            n = linkDao.getCount(new DaoFilter().setLanguages(language).setDestIds(wpId));
        } else {
            n = linkDao.getCount(new DaoFilter().setLanguages(language).setSourceIds(wpId));
        }
        synchronized (linkCounts) {
            linkCounts.put(wpId, n);
        }
        return n;
    }


    @Override
    public TIntFloatMap getVector(String phrase) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Explanation> getExplanations(String phrase1, String phrase2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Explanation> getExplanations(int pageID1, int pageID2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        LocalPage page1=pageDao.getById(language,pageID1);
        LocalPage page2=pageDao.getById(language,pageID2);
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

    public void setWeightByPopularity(boolean weightByPopularity) {
        this.weightByPopularity = weightByPopularity;
    }

    public void setLogTransform(boolean logTransform) {
        this.logTransform = logTransform;
    }

    public static class Provider extends org.wikibrain.conf.Provider<SparseVectorGenerator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SparseVectorGenerator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.sparsegenerator";
        }

        @Override
        public SparseVectorGenerator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("links")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            try {
                LinkGenerator lg = new LinkGenerator(
                            language,
                        getConfigurator().get(LocalLinkDao.class),
                        getConfigurator().get(LocalPageDao.class),
                        config.getBoolean("outLinks"),
                        getConfig().get().getString("sr.blacklist.path")

                        );
                if (config.hasPath("weightByPopularity")) {
                    lg.setWeightByPopularity(config.getBoolean("weightByPopularity"));
                }
                if (config.hasPath("logTransform")) {
                    lg.setLogTransform(config.getBoolean("logTransform"));
                }

                return lg;
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
