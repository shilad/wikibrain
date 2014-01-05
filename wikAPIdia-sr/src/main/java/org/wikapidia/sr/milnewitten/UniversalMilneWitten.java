package org.wikapidia.sr.milnewitten;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.UniversalLinkDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.pairwise.PairwiseSimilarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Matt Lesicko
 * Date: 7/3/13
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class UniversalMilneWitten extends BaseUniversalSRMetric {
    private UniversalLinkDao universalLinkDao;
    private boolean outLinks;
    private MilneWittenCore core;
    private Integer numArticles = null;


    public UniversalMilneWitten(Disambiguator disambiguator, UniversalPageDao universalPageDao, int algorithmId, UniversalLinkDao universalLinkDao){
        this(disambiguator, universalPageDao, algorithmId, universalLinkDao,false);
    }

    public UniversalMilneWitten(Disambiguator disambiguator, UniversalPageDao universalPageDao, int algorithmId, UniversalLinkDao universalLinkDao,  boolean outLinks){
        super (disambiguator,universalPageDao,algorithmId);
        this.universalLinkDao = universalLinkDao;
        this.outLinks = outLinks;
        this.core = new MilneWittenCore();
    }

    @Override
    public String getName() {
        return "UniversalMilneWitten";
    }

    public boolean isOutLinks() {
        return outLinks;
    }

    public void setOutLinks(boolean outLinks) {
        this.outLinks = outLinks;
    }


    @Override
    public SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations) throws DaoException {
        if (page1.getAlgorithmId() != page2.getAlgorithmId()){
            throw new IllegalArgumentException();
        }
        int algorithmId = page1.getAlgorithmId();

        TIntSet a = getLinks(page1.getUnivId(), algorithmId);
        TIntSet b = getLinks(page2.getUnivId(), algorithmId);

        if (numArticles == null) {
            DaoFilter daoFilter = new DaoFilter().setAlgorithmIds(algorithmId);
            numArticles = universalPageDao.getCount(daoFilter);
        }

        SRResult result = core.similarity(a,b,numArticles,explanations);
        result.setId(page2.getUnivId());

        if (explanations) {
            result.setExplanations(reformatExplanations(result.getExplanations(),page1,page2));
        }

        return normalize(result);
    }

    @Override
    public SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations) throws DaoException {
        return super.similarity(phrase1,phrase2,explanations);
    }

    @Override
    public SRResultList mostSimilar(UniversalPage page, int maxResults) throws DaoException{
        return mostSimilar(page,maxResults,null);
    }


    @Override
    public SRResultList mostSimilar(UniversalPage page, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarUniversal(page.getUnivId())){
            SRResultList mostSimilar= getCachedMostSimilarUniversal(page.getUnivId(), maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            //Only check pages that share at least one inlink/outlink.
            TIntSet linkPages = getLinks(page.getUnivId(), algorithmId);
            TIntIntMap worthChecking = new TIntIntHashMap();
            for (int id : linkPages.toArray()){
                TIntSet links;
                if (outLinks){
                    links = universalLinkDao.getInlinkIds(id,algorithmId);
                } else {
                    links = universalLinkDao.getOutlinkIds(id,algorithmId);
                }
                for (int link : links.toArray()){
                    if (validIds==null||validIds.contains(link)){
                        worthChecking.adjustOrPutValue(link,1,1);
                    }
                }
            }
            //Don't try to check red links.
            if (worthChecking.containsKey(-1)){
                worthChecking.remove(-1);
            }

            return mostSimilarFromKnown(page, maxResults, worthChecking);
        }
    }

    /**
     * This is an unoptimized mostSimilar method. It should never get called except from
     * the mostSimilar methods that create a list of IDs that is worth checking.
     * @param page
     * @param maxResults
     * @param worthChecking the only IDs that will be checked. These should be generated from a list of ids known to be similar.
     * @return
     * @throws DaoException
     */
    private SRResultList mostSimilarFromKnown(UniversalPage page, int maxResults, TIntIntMap worthChecking) throws DaoException {
        if (worthChecking==null){
            return new SRResultList(maxResults);
        }
        int pageLinks = getNumLinks(page.getUnivId(), algorithmId, outLinks);

        if (numArticles == null) {
            DaoFilter daoFilter = new DaoFilter().setAlgorithmIds(algorithmId);
            numArticles = universalPageDao.getCount(daoFilter);
        }

        List<SRResult> results = new ArrayList<SRResult>();
        for (int id : worthChecking.keys()){
            int comparisonLinks = getNumLinks(id, algorithmId, outLinks);
            SRResult result = new SRResult(id, 1.0-(
                    (Math.log(Math.max(pageLinks,comparisonLinks))
                            -Math.log(worthChecking.get(id)))
                            / (Math.log(numArticles)
                            - Math.log(Math.min(pageLinks,comparisonLinks)))));
            results.add(result);
        }
        Collections.sort(results);
        Collections.reverse(results);

        SRResultList  resultList = new SRResultList(maxResults);
        for (int i=0; i<maxResults&&i<results.size(); i++){
            resultList.set(i,results.get(i));
        }

        return normalize(resultList);
    }

    @Override
    public TIntDoubleMap getVector(int id) throws DaoException {
        TIntDoubleMap vector = new TIntDoubleHashMap();
        TIntSet links;
        if (outLinks){
            links = universalLinkDao.getOutlinkIds(id,algorithmId);
        } else {
            links = universalLinkDao.getInlinkIds(id,algorithmId);
        }
        for (int link : links.toArray()){
            vector.put(link,1);
        }
        return vector;
    }

    /**
     * Reformat the explanations returned by MilneWittenCore to fit UniversalPages
     * @param explanations
     * @param page1
     * @param page2
     * @return
     * @throws DaoException
     */
    private List<Explanation> reformatExplanations(List<Explanation> explanations, UniversalPage page1, UniversalPage page2) throws DaoException {
        List<Explanation> explanationList = new ArrayList<Explanation>();
        if (outLinks){
            for (Explanation explanation : explanations){
                String format = "Both ? and ? link to ?";
                int id = (Integer)explanation.getInformation().get(0);
                UniversalPage intersectionPage = universalPageDao.getById(id,algorithmId);
                if (intersectionPage==null){
                    continue;
                }
                List<UniversalPage> formatPages = new ArrayList<UniversalPage>();
                formatPages.add(page1);
                formatPages.add(page2);
                formatPages.add(intersectionPage);
                explanationList.add(new Explanation(format, formatPages));
            }
        }
        else{
            for (Explanation explanation : explanations){
                String format = "? links to both ? and ?";
                int id = (Integer)explanation.getInformation().get(0);
                UniversalPage intersectionPage = universalPageDao.getById(id,algorithmId);
                if (intersectionPage==null){
                    continue;
                }
                List<UniversalPage> formatPages = new ArrayList<UniversalPage>();
                formatPages.add(intersectionPage);
                formatPages.add(page1);
                formatPages.add(page2);
                explanationList.add(new Explanation(format, formatPages));
            }
        }
        return explanationList;
    }

    private TIntSet getLinks(int universeId, int algorithmId) throws DaoException {
        TIntSet linkIds = new TIntHashSet();
        if(!outLinks) {
            TIntSet links = universalLinkDao.getInlinkIds(universeId,algorithmId);
            for (Integer link : links.toArray()){
                linkIds.add(link);
            }
        } else {
            TIntSet links = universalLinkDao.getOutlinkIds(universeId, algorithmId);
            for (Integer link : links.toArray()){
                linkIds.add(link);
            }
        }
        return linkIds;
    }

    private int getNumLinks(int universeId, int algorithmId, boolean outLinks) throws DaoException {
        DaoFilter daoFilter = new DaoFilter().setAlgorithmIds(algorithmId);
        if (outLinks){
            daoFilter.setSourceIds(universeId);
        }
        else {
            daoFilter.setDestIds(universeId);
        }
        return universalLinkDao.getCount(daoFilter);
    }

    @Override
    public void writeCosimilarity(String path, int maxHits) throws IOException, DaoException, WikapidiaException{
//        PairwiseSimilarity pairwiseSimilarity = new PairwiseMilneWittenSimilarity();
//        super.writeCosimilarity(path, maxHits, pairwiseSimilarity);
    }

    @Override
    public void readCosimilarity(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalSRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.universal";
        }

        @Override
        public UniversalSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("UniversalMilneWitten")) {
                return null;
            }

            UniversalSRMetric usr = new UniversalMilneWitten(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(UniversalPageDao.class,config.getString("pageDao")),
                    Env.getUniversalConceptAlgorithmId(getConfig()),
                    getConfigurator().get(UniversalLinkDao.class,config.getString("linkDao")),
                    config.getBoolean("outLinks")
            );
            try {
                usr.read(getConfig().get().getString("sr.metric.path"));
            } catch (IOException e){
                usr.setSimilarityNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")));
                usr.setMostSimilarNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")));
            }
            try {
                usr.readCosimilarity(getConfig().get().getString("sr.metric.path"));
            } catch (IOException e){}
            return usr;
        }
    }
}
