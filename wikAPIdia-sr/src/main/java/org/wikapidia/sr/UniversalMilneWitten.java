package org.wikapidia.sr;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
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
        return "Milne Witten";
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

        DaoFilter pageFilter = new DaoFilter();
        Iterable<UniversalPage> allPages = universalPageDao.get(pageFilter);
        int numArticles = 0;
        for (UniversalPage page : allPages){
            numArticles++;
        }

        SRResult result = core.similarity(A,B,numArticles,explanations);

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
                Set<Integer> links;
                if (outLinks){
                    links = universalLinkDao.getInlinks(id,algorithmId).getLinks().keySet();
                } else {
                    links = universalLinkDao.getOutlinks(id,algorithmId).getLinks().keySet();
                }
                for (int link : links){
                    worthChecking.add(link);
                }
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

            DaoFilter pageFilter = new DaoFilter();
            Iterable<UniversalPage> allPages = universalPageDao.get(pageFilter);
            int numArticles = 0;
            for (UniversalPage up : allPages){
                numArticles++;
            }

            List<SRResult> results = new ArrayList<SRResult>();
            for (int id : validIds.toArray()){
                TIntSet comparisonLinks = getLinks(id, algorithmId);
                SRResult result = core.similarity(pageLinks, comparisonLinks, numArticles, explanations);
                if (explanations){
                    UniversalPage up = universalPageDao.getById(id,algorithmId);
                    result.setExplanations(reformatExplanations(result.getExplanations(),page,up));
                }
                results.add(result);
            }
            Collections.sort(results);

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
        Map<Integer, UniversalLink> links;
        if (outLinks){
            links = universalLinkDao.getOutlinks(id,algorithmId).getLinks();
        } else {
            links = universalLinkDao.getInlinks(id,algorithmId).getLinks();
        }
        DaoFilter pageFilter = new DaoFilter();
        Iterable<UniversalPage> allPages = universalPageDao.get(pageFilter);
        for (UniversalPage page : allPages){
            if (links.containsKey(page.getUnivId())){
                vector.put(page.getUnivId(),1);
            }
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
        if (outLinks){
            List<Explanation> explanationList = new ArrayList<Explanation>();
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
            List<Explanation> explanationList = new ArrayList<Explanation>();
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
        return explanations;
    }

    private TIntSet getLinks(int universeId, int algorithmId) throws DaoException {
        TIntSet linkIds = new TIntHashSet();
        if(!outLinks) {
            Map<Integer, UniversalLink> links = universalLinkDao.getInlinks(universeId,algorithmId).getLinks();
            for (Integer link : links.keySet()){
                linkIds.add(links.get(link).getSourceUnivId());
            }
        } else {
            Map<Integer, UniversalLink> links = universalLinkDao.getOutlinks(universeId, algorithmId).getLinks();
            for (Integer link : links.keySet()){
                linkIds.add(links.get(link).getDestUnivId());
            }
        }
        return linkIds;
    }
}
