package org.wikapidia.sr;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;

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

    public String getName() {
        return "LocalMilneWitten";
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
    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        if (page1.getLanguage()!=page2.getLanguage()){
            throw new IllegalArgumentException();
        }

        TIntSet A = getLinks(new LocalId(page1.getLanguage(), page1.getLocalId()),outLinks);
        TIntSet B = getLinks(new LocalId(page2.getLanguage(), page2.getLocalId()),outLinks);

        int numArticles;
        if (numPages.containsKey(page1.getLanguage())) {
            numArticles = numPages.get(page1.getLanguage());
        } else {
            DaoFilter pageFilter = new DaoFilter().setLanguages(page1.getLanguage());
            numArticles = pageHelper.getCount(pageFilter);
            numPages.put(page1.getLanguage(), numArticles);
        }

        SRResult result = core.similarity(A,B,numArticles,explanations);
        result.id = page2.getLocalId();

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
            TIntSet worthChecking = new TIntHashSet();
            for (int id : linkPages.toArray()){
                TIntSet links = getLinks(new LocalId(page.getLanguage(), id), !outLinks);
                for (int link: links.toArray()){
                     worthChecking.add(link);
                }
            }

            //Don't try to check red links.
            if (worthChecking.contains(-1)){
                worthChecking.remove(-1);
            }

            if (validIds!=null){
                worthChecking.retainAll(validIds);
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
    private SRResultList mostSimilarFromKnown(LocalPage page, int maxResults, TIntSet worthChecking) throws DaoException {
        if (worthChecking==null){
            return new SRResultList(maxResults);
        }

        TIntSet pageLinks = getLinks(page.toLocalId(),outLinks);

        int numArticles;
        if (numPages.containsKey(page.getLanguage())) {
            numArticles = numPages.get(page.getLanguage());
        } else {
            DaoFilter pageFilter = new DaoFilter().setLanguages(page.getLanguage());
            numArticles = pageHelper.getCount(pageFilter);
            numPages.put(page.getLanguage(), numArticles);
        }

        List<SRResult> results = new ArrayList<SRResult>();
        for (int id : worthChecking.toArray()){
            TIntSet comparisonLinks = getLinks(new LocalId(page.getLanguage(),id),outLinks);
            SRResult result = core.similarity(pageLinks, comparisonLinks, numArticles,false);
            result.id=id;
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
            if (!config.getString("type").equals("LocalMilneWitten")) {
                return null;
            }

            LocalSRMetric sr = new LocalMilneWitten(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalLinkDao.class,config.getString("linkDao")),
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    config.getBoolean("outLinks")
            );
            try {
                sr.read(getConfig().get().getString("sr.metric.path"));
            } catch (IOException e){
                sr.setDefaultSimilarityNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                sr.setDefaultMostSimilarNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                List<String> langCodes = getConfig().get().getStringList("languages");
                for (String langCode : langCodes){
                    Language language = Language.getByLangCode(langCode);
                    sr.setSimilarityNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                    sr.setMostSimilarNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                }
            }
            return sr;
        }

    }
}
