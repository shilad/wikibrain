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
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.pairwise.PairwiseMilneWittenSimilarity;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Matt Lesicko
 */

public class MonolingualMilneWitten extends BaseMonolingualSRMetric {
    LocalLinkDao linkHelper;
    //False is standard Milne Witten with in links, true is with out links
    private boolean outLinks;
    private MilneWittenCore core;
    private String name = "localmilnewitten";
    private Integer numPages;


    public MonolingualMilneWitten(Language language, LocalPageDao pageHelper, Disambiguator disambiguator, LocalLinkDao linkHelper) {
        this(language, pageHelper, disambiguator,linkHelper, false);
    }

    public MonolingualMilneWitten(Language language, LocalPageDao pageHelper, Disambiguator disambiguator, LocalLinkDao linkHelper, boolean outLinks) {
        super(language,pageHelper,disambiguator);
        this.linkHelper = linkHelper;
        this.outLinks = outLinks;
        this.core = new MilneWittenCore();
        numPages = null;
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
    public TIntDoubleMap getVector(int id) throws DaoException {
        TIntDoubleMap vector = new TIntDoubleHashMap();
        TIntSet links = getLinks(id,outLinks);

        for (int link : links.toArray()) {
            vector.put(link,1);
        }
        return vector;
    }



    //TODO: Add a normalizer
    //TODO: similarity -> relatedness
    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        TIntSet a = getLinks(pageId1,outLinks);
        TIntSet b = getLinks(pageId2,outLinks);

        if (numPages==null) {
            DaoFilter pageFilter = new DaoFilter().setLanguages(getLanguage()).setRedirect(false);
            numPages = getLocalPageDao().getCount(pageFilter);
        }

        SRResult result = core.similarity(a,b,numPages,explanations);
        result.setId(pageId2);

        //Reformat explanations to fit our metric.
        if (explanations) {
            result.setExplanations(reformatExplanations(result.getExplanations(),pageId1,pageId2));
        }

        return normalize(result);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
        return mostSimilar(pageId,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarLocal(pageId)){
            SRResultList mostSimilar= getCachedMostSimilarLocal( pageId, maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            //Only check pages that share at least one inlink/outlink.
            TIntSet linkPages = getLinks(pageId,outLinks);
            TIntIntMap worthChecking = new TIntIntHashMap();
            for (int id : linkPages.toArray()){
                TIntSet links = getLinks(id, !outLinks);
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

            return mostSimilarFromKnown(pageId, maxResults,worthChecking);
        }
    }

    /**
     * This is an unoptimized mostSimilar method. It should never get called except from
     * the mostSimilar methods that create a list of IDs that is worth checking.
     * @param pageId
     * @param maxResults
     * @param worthChecking the only IDs that will be checked. These should be generated from a list of ids known to be similar.
     * @return
     * @throws org.wikapidia.core.dao.DaoException
     */
    private SRResultList mostSimilarFromKnown(int pageId, int maxResults, TIntIntMap worthChecking) throws DaoException {
        if (worthChecking==null){
            return new SRResultList(maxResults);
        }

        int pageLinks = getNumLinks(pageId,outLinks);

        if (numPages==null) {
            DaoFilter pageFilter = new DaoFilter().setLanguages(getLanguage()).setRedirect(false);
            numPages = getLocalPageDao().getCount(pageFilter);
        }

        List<SRResult> results = new ArrayList<SRResult>();
        for (int id : worthChecking.keys()){
            int comparisonLinks = getNumLinks(id, outLinks);
            SRResult result = new SRResult(id, 1.0-(
                    (Math.log(Math.max(pageLinks,comparisonLinks))
                            -Math.log(worthChecking.get(id)))
                            / (Math.log(numPages)
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

    private List<Explanation> reformatExplanations(List<Explanation> explanations, int page1, int page2) throws DaoException {
        LocalPageDao pageDao = getLocalPageDao();
        List<Explanation> explanationList = new ArrayList<Explanation>();
        if (outLinks){
            for (Explanation explanation : explanations){
                String format = "Both ? and ? link to ?";
                int id = (Integer)explanation.getInformation().get(0);
                LocalPage intersectionPage = pageDao.getById(getLanguage(),id);
                if (intersectionPage==null){
                    continue;
                }
                List<LocalPage> formatPages = new ArrayList<LocalPage>();
                formatPages.add(pageDao.getById(getLanguage(),page1));
                formatPages.add(pageDao.getById(getLanguage(),page2));
                formatPages.add(intersectionPage);
                explanationList.add(new Explanation(format, formatPages));
            }
        }
        else{
            for (Explanation explanation : explanations){
                String format = "? links to both ? and ?";
                int id = (Integer)explanation.getInformation().get(0);
                LocalPage intersectionPage = pageDao.getById(getLanguage(),id);
                if (intersectionPage==null){
                    continue;
                }
                List<LocalPage> formatPages = new ArrayList<LocalPage>();
                formatPages.add(intersectionPage);
                formatPages.add(pageDao.getById(getLanguage(),page1));
                formatPages.add(pageDao.getById(getLanguage(),page2));
                explanationList.add(new Explanation(format, formatPages));
            }
        }
        return explanationList;
    }

    private TIntSet getLinks(int id, boolean outLinks) throws DaoException {
        Iterable<LocalLink> links = linkHelper.getLinks(getLanguage(), id, outLinks);
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

    private int getNumLinks(int id, boolean outLinks) throws DaoException {
        DaoFilter daoFilter = new DaoFilter().setLanguages(getLanguage());
//        if (outLinks){
//            daoFilter.setDestIds(id.getId());
//        } else {
//            daoFilter.setSourceIds(id.getId());
//        }
        if (outLinks){
            daoFilter.setSourceIds(id);
        } else {
            daoFilter.setDestIds(id);
        }
        return linkHelper.getCount(daoFilter);
    }

    @Override
    public void writeCosimilarity(String path, int maxHits) throws IOException, DaoException, WikapidiaException{
        super.writeCosimilarity(path, maxHits, new PairwiseMilneWittenSimilarity());
    }


    @Override
    public void readCosimilarity(String path) throws IOException {
        super.readCosimilarity(path, new PairwiseMilneWittenSimilarity());
    }

    public static class Provider extends org.wikapidia.conf.Provider<MonolingualSRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return MonolingualSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public MonolingualSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("milnewitten")) {
                return null;
            }

            if (!runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("Monolingual requires 'language' runtime parameter.");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));

            MonolingualMilneWitten sr = new MonolingualMilneWitten(
                    language, getConfigurator().get(LocalPageDao.class,config.getString("pageDao")), getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalLinkDao.class,config.getString("linkDao")),
                    config.getBoolean("outLinks")
            );
            sr.setName(name);
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
