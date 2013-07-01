package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.mapper.ConceptMapper;
import org.wikapidia.sr.utils.KnownSim;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MilneWittenInLinkSimilarity extends BaseLocalSRMetric{
    LocalLinkDao linkHelper;
    LocalPageDao pageHelper;

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

    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds) {
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

    public MilneWittenInLinkSimilarity(ConceptMapper mapper, LocalLinkDao linkHelper, LocalPageDao pageHelper) {
        this.linkHelper = linkHelper;
        this.pageHelper = pageHelper;
    }

    //TODO: normalize!
    @Override
    public double similarity(int wpId1, int wpId2) throws IOException {
        TIntSet A = getInLinks(wpId1);
        TIntSet B = getInLinks(wpId2);
        if (A == null || B == null) {
            return Double.NaN;
        }
        TIntSet I = new TIntHashSet(A); I.retainAll(B); // intersection
        int numArticles = linkHelper.getReader().numDocs();

//        System.out.println("sizes are A=" + A.size() + ", B=" + B.size() + " I=" + I.size());
        if (I.size() == 0) {
            return 0;
        }

        return 1.0 - (
            (Math.log(Math.max(A.size(), B.size())) - Math.log(I.size()))
        /   (Math.log(numArticles) - Math.log(Math.min(A.size(), B.size()))));
    }

    private TIntSet getInLinks(int wpId) throws IOException {
        Document d = linkHelper.wpIdToLuceneDoc(wpId);
        if (d == null) {
//            Document d2 = getHelper().wpIdToLuceneDoc(wpId);
//            if (d2 != null) {
//                System.err.println("missing article " + wpId + " with title + " + d2.get("title") + " and type " + d2.get("type"));
//            }
            return null;
        }
        TIntSet links = new TIntHashSet();
        for (IndexableField f : d.getFields(Page.FIELD_INLINKS)) {
            if (linkHelper.getDocFreq(Page.FIELD_INLINKS, f.stringValue()) >= 3) {
                links.add(Integer.valueOf(f.stringValue()));
            }
        }
        return links;
    }

    @Override
    public DocScoreList mostSimilar(int wpId1, int maxResults, TIntSet validIds) throws IOException {
        throw new NotImplementedException();
    }
}
