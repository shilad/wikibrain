package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class ESAMetric extends BaseLocalSRMetric {
    public ESAMetric(Disambiguator disambiguator) {
        super(disambiguator);
    }

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    @Override
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException {
        // Get result vector for phrase 1

        // Get result vector for phrase 2

        // Compare vectors according to the method described in the ESA paper.

        return null;
    }

        @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) {
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
    public void write(File directory) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
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
    public double[][] cosimilarity(String[] phrases, Language language) {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
