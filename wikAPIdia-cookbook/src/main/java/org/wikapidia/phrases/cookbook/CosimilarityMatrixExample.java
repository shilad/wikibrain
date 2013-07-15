package org.wikapidia.phrases.cookbook;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.pairwise.SRFeatureMatrixWriter;

import java.io.IOException;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class CosimilarityMatrixExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, WikapidiaException, InterruptedException, IOException {
        //Set-up
        Configurator c = new Configurator(new Configuration());

        //Local pairwise cosimilarity
        LocalSRMetric sr = c.get(LocalSRMetric.class);
        LanguageSet languages = new LanguageSet("simple");
        sr.writeCosimilarity(languages,8,100);

        //Universal pairwise cosimilarity matrix
        UniversalSRMetric usr = c.get(UniversalSRMetric.class);
        usr.writeCosimilarity(8,100);

    }
}
