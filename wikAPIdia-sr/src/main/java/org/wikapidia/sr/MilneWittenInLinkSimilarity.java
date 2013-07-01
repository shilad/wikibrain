package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.mapper.ConceptMapper;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;

public class MilneWittenInLinkSimilarity extends BaseLocalSRMetric{
    IndexHelper linkHelper;

    public String getName() {
        return "Milne Witten";
    }



    public MilneWittenInLinkSimilarity(ConceptMapper mapper, IndexHelper linkHelper, IndexHelper mainHelper) {
        super(mapper, mainHelper);
        this.linkHelper = linkHelper;
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
