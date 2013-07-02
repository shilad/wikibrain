package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.KnownSim;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MilneWittenSimilarity extends BaseLocalSRMetric{
    LocalLinkDao linkHelper;
    LocalPageDao pageHelper;
    //False is standard Milne Witten with in links, true is with out links
    private boolean outLinks = false;

    public MilneWittenSimilarity(Disambiguator disambiguator, LocalLinkDao linkHelper, LocalPageDao pageHelper) {
        super(disambiguator);
        this.linkHelper = linkHelper;
        this.pageHelper = pageHelper;
    }

    public MilneWittenSimilarity(Disambiguator disambiguator, LocalLinkDao linkHelper, LocalPageDao pageHelper, boolean outLinks) {
        super(disambiguator);
        this.linkHelper = linkHelper;
        this.pageHelper = pageHelper;
        this.outLinks = outLinks;
    }

    public boolean isOutLinks() {
        return outLinks;
    }

    public void setOutLinks(boolean outLinks) {
        this.outLinks = outLinks;
    }

    public String getName() {
        return "Milne Witten";
    }

    public void read(File directory) throws IOException {
        throw new NotImplementedException();
    }

    public void write(File directory) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void trainSimilarity(List<KnownSim> labeled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainMostSimilar(List<KnownSim> labeled, int numResults, TIntSet validIds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(String[] phrases, Language language) {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO: Add a normalizer
    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        if (page1.getLanguage()!=page2.getLanguage()){
            return new SRResult(Double.NaN);
        }
        TIntSet A = getLinks(new LocalId(page1.getLanguage(), page1.getLocalId()));
        TIntSet B = getLinks(new LocalId(page2.getLanguage(), page2.getLocalId()));

        //Error handling for null pages
        if (A == null || B == null) {
            return new SRResult(Double.NaN);
        }

        TIntSet I = new TIntHashSet(A); I.retainAll(B); // intersection
        DaoFilter pageFilter = new DaoFilter().setLanguages(page1.getLanguage());
        Iterable<LocalPage> allPages = pageHelper.get(pageFilter);
        int numArticles = 0;
        for (LocalPage page : allPages){
            numArticles++;
        }

        if (I.size() == 0) {
            return new SRResult(0.0);
        }

        SRResult result = new SRResult(1.0 - (
            (Math.log(Math.max(A.size(), B.size())) - Math.log(I.size()))
            /   (Math.log(numArticles) - Math.log(Math.min(A.size(), B.size())))));

//        SRResult result = new SRResult(1.0 - (
//            (Math.max(A.size(), B.size()) - I.size()*1.0)
//            /   (numArticles - Math.min(A.size(), B.size()))));

        if (explanations) {
            for (int id : I.toArray()) {
                String format = "? links to both ? and ?";
                LocalPage intersectionPage = pageHelper.getById(page1.getLanguage(), id);
                List<LocalPage> formatPages =new ArrayList<LocalPage>();
                formatPages.add(intersectionPage);
                formatPages.add(page1);
                formatPages.add(page2);
                Explanation explanation = new Explanation(format, formatPages);
                result.addExplanation(explanation);
            }
        }

        return result;
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations) {
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



    private TIntSet getLinks(LocalId wpId) throws DaoException {
        SqlDaoIterable<LocalLink> links = linkHelper.getLinks(wpId.getLanguage(), wpId.getId(), outLinks);
        TIntSet linkIds = new TIntHashSet();
        for (LocalLink link : links){
            linkIds.add(link.getSourceId());
        }
        return linkIds;
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds) {
        throw new NotImplementedException();
    }
}
