package org.wikapidia.sr;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.*;
import org.wikapidia.sr.disambig.Disambiguator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public UniversalMilneWitten(Disambiguator disambiguator, UniversalLinkDao universalLinkDao, UniversalPageDao universalPageDao){
        this(disambiguator, universalLinkDao, universalPageDao,false);
    }

    public UniversalMilneWitten(Disambiguator disambiguator, UniversalLinkDao universalLinkDao, UniversalPageDao universalPageDao, boolean outLinks){
        this.disambiguator = disambiguator;
        this.universalLinkDao = universalLinkDao;
        this.universalPageDao = universalPageDao;
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
            return new SRResult(Double.NaN);
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

        //Reformat explanations to fit our metric.
        if (explanations) {
            if (outLinks){
                List<Explanation> explanationList = new ArrayList<Explanation>();
                for (Explanation explanation : result.getExplanations()){
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
                result.setExplanations(explanationList);
            }
            else{
                List<Explanation> explanationList = new ArrayList<Explanation>();
                for (Explanation explanation : result.getExplanations()){
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
                result.setExplanations(explanationList);
            }
        }

        return result;
    }

    @Override
    public SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations, TIntSet validIds) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations, TIntSet validIds) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(int[] rowIds, int[] colIds) throws IOException {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(LocalString[] rowPhrases, LocalString[] colPhrases) throws IOException {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(LocalString[] phrases) throws IOException {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TIntDoubleMap getVector(int id, int algorithmId) throws DaoException {
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
            vector.put(page.getUnivId(),links.containsKey(page.getUnivId())? 1: 0);
        }
        return vector;
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
