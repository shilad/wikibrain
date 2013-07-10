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
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.pairwise.SRFeatureMatrixWriter;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class CosimilarityMatrixExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, WikapidiaException, InterruptedException {
        //Set-up
        Configurator c = new Configurator(new Configuration());
        SRFeatureMatrixWriter writer = c.get(SRFeatureMatrixWriter.class);
        LocalPageDao localPageDao = c.get(LocalPageDao.class);

        //Write the feature matrix on all ids from simple english
        Iterable<LocalPage> localPages = localPageDao.get(new DaoFilter());
        TIntSet pageIds = new TIntHashSet();
        for (LocalPage page : localPages) {
            if (page != null) {
            pageIds.add(page.getLocalId());
            }
        }
        writer.writeFeatureVectors(pageIds.toArray(), 4);


    }




}
