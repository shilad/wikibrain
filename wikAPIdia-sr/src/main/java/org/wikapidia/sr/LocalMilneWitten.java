package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.KnownSim;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalMilneWitten extends BaseLocalSRMetric{
    LocalLinkDao linkHelper;
    //False is standard Milne Witten with in links, true is with out links
    private boolean outLinks;
    private MilneWittenCore core;

    public LocalMilneWitten(Disambiguator disambiguator, LocalLinkDao linkHelper, LocalPageDao pageHelper) {
        this(disambiguator,linkHelper,pageHelper,false);
    }

    public LocalMilneWitten(Disambiguator disambiguator, LocalLinkDao linkHelper, LocalPageDao pageHelper, boolean outLinks) {
        this.disambiguator = disambiguator;
        this.linkHelper = linkHelper;
        this.pageHelper = pageHelper;
        this.outLinks = outLinks;
        this.core = new MilneWittenCore();
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
    public SparseMatrixRow getVector(int id) {


        return null;
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

        DaoFilter pageFilter = new DaoFilter().setLanguages(page1.getLanguage());
        Iterable<LocalPage> allPages = pageHelper.get(pageFilter);
        int numArticles = 0;
        for (LocalPage page : allPages){
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
                    LocalPage intersectionPage = pageHelper.getById(page1.getLanguage(),id);
                    if (intersectionPage==null){
                        continue;
                    }
                    List<LocalPage> formatPages = new ArrayList<LocalPage>();
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
                    LocalPage intersectionPage = pageHelper.getById(page1.getLanguage(),id);
                    if (intersectionPage==null){
                        continue;
                    }
                    List<LocalPage> formatPages = new ArrayList<LocalPage>();
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
        if(!outLinks) {
            for (LocalLink link : links){
            linkIds.add(link.getSourceId());
            }
        } else {
            for (LocalLink link : links){
                linkIds.add(link.getDestId());
            }
        }
        return linkIds;
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds) {
        throw new NotImplementedException();
    }
}
