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
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.pairwise.PairwiseMilneWittenSimilarity;

import java.io.IOException;
import java.util.*;

/**
 * @author Ben Hillmann
 * @author Matt Lesicko
 */

public class LocalMilneWitten extends BaseLocalSRMetric {
    LocalLinkDao linkHelper;
    //False is standard Milne Witten with in links, true is with out links
    private boolean outLinks;
    private MilneWittenCore core;
    private String name = "localmilnewitten";
    private Map<Language,Integer> numPages = new HashMap<Language, Integer>();


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

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        TIntDoubleMap vector = new TIntDoubleHashMap();
        TIntSet links = getLinks(new LocalId(language, id),outLinks);

        for (int link : links.toArray()) {
            vector.put(link,1);
        }
        return vector;
    }



    //TODO: Add a normalizer
    //TODO: similarity -> relatedness
    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        if (page1 == null || page2 == null) {
           return new SRResult(Double.NaN);
        }
        if (page1.getLanguage()!=page2.getLanguage()){
            throw new IllegalArgumentException("Tried to compute local similarity of pages in different languages: page1 was in"+page1.getLanguage().getEnLangName()+" and page2 was in "+ page2.getLanguage().getEnLangName());
        }

        TIntSet a = getLinks(new LocalId(page1.getLanguage(), page1.getLocalId()),outLinks);
        TIntSet b = getLinks(new LocalId(page2.getLanguage(), page2.getLocalId()),outLinks);

        int numArticles;
        if (numPages.containsKey(page1.getLanguage())) {
            numArticles = numPages.get(page1.getLanguage());
        } else {
            DaoFilter pageFilter = new DaoFilter().setLanguages(page1.getLanguage()).setRedirect(false);
            numArticles = pageHelper.getCount(pageFilter);
            numPages.put(page1.getLanguage(), numArticles);
        }

        SRResult result = core.similarity(a,b,numArticles,explanations);
        result.setId(page2.getLocalId());

        //Reformat explanations to fit our metric.
        if (explanations) {
            result.setExplanations(reformatExplanations(result.getExplanations(),page1,page2));
        }

        return normalize(result,page1.getLanguage());
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
        return mostSimilar(page,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarLocal(page.getLanguage(), page.getLocalId())){
            SRResultList mostSimilar= getCachedMostSimilarLocal(page.getLanguage(), page.getLocalId(), maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            //Only check pages that share at least one inlink/outlink.
            TIntSet linkPages = getLinks(page.toLocalId(),outLinks);
            TIntIntMap worthChecking = new TIntIntHashMap();
            for (int id : linkPages.toArray()){
                TIntSet links = getLinks(new LocalId(page.getLanguage(), id), !outLinks);
                for (int link: links.toArray()){
                    if (validIds==null||validIds.contains(link)){
                        worthChecking.adjustOrPutValue(link,1,1);
                    }
                }
            }

            //Don't try to check red links.
            if (worthChecking.containsKey(-1)){
                worthChecking.remove(-1);
            }

            return mostSimilarFromKnown(page, maxResults,worthChecking);
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
    private SRResultList mostSimilarFromKnown(LocalPage page, int maxResults, TIntIntMap worthChecking) throws DaoException {
        if (worthChecking==null){
            return new SRResultList(maxResults);
        }

        int pageLinks = getNumLinks(page.toLocalId(),outLinks);

        int numArticles;
        if (numPages.containsKey(page.getLanguage())) {
            numArticles = numPages.get(page.getLanguage());
        } else {
            DaoFilter pageFilter = new DaoFilter().setLanguages(page.getLanguage()).setRedirect(false);
            numArticles = pageHelper.getCount(pageFilter);
            numPages.put(page.getLanguage(), numArticles);
        }

        List<SRResult> results = new ArrayList<SRResult>();
        for (int id : worthChecking.keys()){
            int comparisonLinks = getNumLinks(new LocalId(page.getLanguage(),id), outLinks);
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

        return normalize(resultList,page.getLanguage());
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

    private TIntSet getLinks(LocalId wpId, boolean outLinks) throws DaoException {
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

    private int getNumLinks(LocalId id, boolean outLinks) throws DaoException {
        DaoFilter daoFilter = new DaoFilter().setLanguages(id.getLanguage());
//        if (outLinks){
//            daoFilter.setDestIds(id.getId());
//        } else {
//            daoFilter.setSourceIds(id.getId());
//        }
        if (outLinks){
            daoFilter.setSourceIds(id.getId());
        } else {
            daoFilter.setDestIds(id.getId());
        }
        return linkHelper.getCount(daoFilter);
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException{
        super.writeCosimilarity(path, languages, maxHits, new PairwiseMilneWittenSimilarity());
    }


    @Override
    public void readCosimilarity(String path, LanguageSet languages) throws IOException {
        super.readCosimilarity(path, languages, new PairwiseMilneWittenSimilarity());
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
        public LocalSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("milnewitten")) {
                return null;
            }

            LocalMilneWitten sr = new LocalMilneWitten(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalLinkDao.class,config.getString("linkDao")),
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    config.getBoolean("outLinks")
            );
            sr.setName(name);
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
