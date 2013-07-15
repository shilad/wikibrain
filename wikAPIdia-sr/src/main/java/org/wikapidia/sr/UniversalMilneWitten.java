package org.wikapidia.sr;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.*;
import org.wikapidia.sr.disambig.Disambiguator;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matt Lesicko
 * Date: 7/3/13
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class UniversalMilneWitten extends BaseUniversalSRMetric{
    private UniversalLinkDao universalLinkDao;
    private boolean outLinks;
    private MilneWittenCore core;

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
        return "MilneWitten";
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

        TIntSet A = getLinks(page1.getUnivId(), algorithmId);
        TIntSet B = getLinks(page2.getUnivId(), algorithmId);

        int numArticles = universalPageDao.getNumPages(page1.getAlgorithmId());

        SRResult result = core.similarity(A,B,numArticles,explanations);
        result.id = page2.getUnivId();

        if (explanations) {
            result.setExplanations(reformatExplanations(result.getExplanations(),page1,page2));
        }

        return result;
    }

    @Override
    public SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations) throws DaoException {
        return super.similarity(phrase1,phrase2,explanations);
    }


    @Override
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations) throws DaoException {
        SRResultList mostSimilar;
        if (hasCachedMostSimilarUniversal(page.getUnivId())&&!explanations){
            mostSimilar= getCachedMostSimilarUniversal(page.getUnivId(), maxResults, null);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            //Only check pages that share at least one inlink/outlink.
            TIntSet linkPages = getLinks(page.getUnivId(), algorithmId);
            TIntSet worthChecking = new TIntHashSet();
            for (int id : linkPages.toArray()){
                TIntSet links;
                if (outLinks){
                    links = universalLinkDao.getInlinkIds(id,algorithmId);
                } else {
                    links = universalLinkDao.getOutlinkIds(id,algorithmId);
                }
                for (int link : links.toArray()){
                    worthChecking.add(link);
                }
            }
            //Don't try to check red links.
            if (worthChecking.contains(-1)){
                worthChecking.remove(-1);
            }
            return mostSimilar(page, maxResults, explanations,worthChecking);
        }
    }

    @Override
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations, TIntSet validIds) throws DaoException {
        if (validIds==null){
            return mostSimilar(page,maxResults,explanations);
        }
        SRResultList mostSimilar;
        if (hasCachedMostSimilarUniversal(page.getUnivId())&&!explanations){
            mostSimilar= getCachedMostSimilarUniversal(page.getUnivId(), maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            TIntSet pageLinks = getLinks(page.getUnivId(), algorithmId);


            int numArticles = universalPageDao.getNumPages(algorithmId);

            List<SRResult> results = new ArrayList<SRResult>();
            for (int id : validIds.toArray()){
                TIntSet comparisonLinks = getLinks(id, algorithmId);
                SRResult result = core.similarity(pageLinks, comparisonLinks, numArticles, explanations);
                result.id = id;
                if (explanations){
                    UniversalPage up = universalPageDao.getById(id,algorithmId);
                    result.setExplanations(reformatExplanations(result.getExplanations(),page,up));
                }
                results.add(result);
            }
            Collections.sort(results);
            Collections.reverse(results);

            SRResultList  resultList = new SRResultList(maxResults);
            for (int i=0; i<maxResults&&i<results.size(); i++){
                resultList.set(i,results.get(i));
            }

            return resultList;
        }
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
            TIntSet links = universalLinkDao.getInlinkIds(universeId,algorithmId);
            for (Integer link : links.toArray()){
                linkIds.add(link);
            }
        }
        return linkIds;
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
        public UniversalSRMetric get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("milneWitten")) {
                return null;
            }

            return new UniversalMilneWitten(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(UniversalPageDao.class,config.getString("pageDao")),
                    config.getInt("algorithmId"),
                    getConfigurator().get(UniversalLinkDao.class,config.getString("linkDao")),
                    config.getBoolean("outLinks")
            );
        }
    }
}
