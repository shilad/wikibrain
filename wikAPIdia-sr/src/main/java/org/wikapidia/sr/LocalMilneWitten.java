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
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.KnownSim;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Ben Hillmann
 * @author Matt Lesicko
 */

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
        return "MilneWitten";
    }

    public void write(File directory) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void read(File directory) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
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
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        TIntDoubleMap vector = new TIntDoubleHashMap();
        TIntSet links = getLinks(new LocalId(language, id));

        for (int link : links.toArray()) {
            vector.put(link,1);
        }

//        DaoFilter pageFilter = new DaoFilter();
//        Iterable<LocalPage> allPages = pageHelper.get(pageFilter);
//        for (LocalPage page: allPages) {
//            if (page != null) {
//            vector.put(page.getLocalId(), links.contains(page.getLocalId()) ? 1F:0F);
//            }
//        }
        return vector;
    }



    //TODO: Add a normalizer
    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        if (page1.getLanguage()!=page2.getLanguage()){
            throw new IllegalArgumentException();
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
        result.id = page2.getLocalId();

        //Reformat explanations to fit our metric.
        if (explanations) {
            result.setExplanations(reformatExplanations(result.getExplanations(),page1,page2));
        }

        return result;
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations) throws DaoException {
        SRResultList mostSimilar;
        if (hasCachedMostSimilarLocal(page.getLanguage(), page.getLocalId())&&!explanations){
            mostSimilar= getCachedMostSimilarLocal(page.getLanguage(), page.getLocalId(), maxResults, null);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            //Only check pages that share at least one inlink/outlink.
            TIntSet linkPages = getLinks(page.toLocalId());
            TIntSet worthChecking = new TIntHashSet();
            for (int id : linkPages.toArray()){
                Iterable<LocalLink> links;
                if (outLinks){
                    links = linkHelper.getLinks(page.getLanguage(),id,false);
                } else {
                    links = linkHelper.getLinks(page.getLanguage(),id,true);
                }
                for (LocalLink link : links){
                    worthChecking.add(link.getLocalId());
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
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds) throws DaoException {
        if (validIds==null){
            return mostSimilar(page,maxResults,explanations);
        }
        SRResultList mostSimilar;
        if (hasCachedMostSimilarLocal(page.getLanguage(), page.getLocalId())&&!explanations){
            mostSimilar= getCachedMostSimilarLocal(page.getLanguage(), page.getLocalId(), maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            TIntSet pageLinks = getLinks(page.toLocalId());

            DaoFilter pageFilter = new DaoFilter().setLanguages(page.getLanguage());
            Iterable<LocalPage> allPages = pageHelper.get(pageFilter);
            int numArticles = 0;
            for (LocalPage lp : allPages){
                numArticles++;
            }

            List<SRResult> results = new ArrayList<SRResult>();
            for (int id : validIds.toArray()){
                TIntSet comparisonLinks = getLinks(new LocalId(page.getLanguage(),id));
                SRResult result = core.similarity(pageLinks, comparisonLinks, numArticles, explanations);
                result.id=id;
                if (explanations){
                    LocalPage lp = pageHelper.getById(page.getLanguage(),id);
                    result.setExplanations(reformatExplanations(result.getExplanations(),page,lp));
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

    private List<Explanation> reformatExplanations(List<Explanation> explanations, LocalPage page1, LocalPage page2) throws DaoException {
        List<Explanation> explanationList = new ArrayList<Explanation>();
        if (outLinks){
            for (Explanation explanation : explanations){
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
        }
        else{
            for (Explanation explanation : explanations){
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
        }
        return explanationList;
    }

    private TIntSet getLinks(LocalId wpId) throws DaoException {
        Iterable<LocalLink> links = linkHelper.getLinks(wpId.getLanguage(), wpId.getId(), outLinks);
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

    public static class Provider extends org.wikapidia.conf.Provider<LocalSRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public LocalSRMetric get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("milneWitten")) {
                return null;
            }

            return new LocalMilneWitten(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalLinkDao.class,config.getString("linkDao")),
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    config.getBoolean("outLinks")
            );
        }


    }
}
