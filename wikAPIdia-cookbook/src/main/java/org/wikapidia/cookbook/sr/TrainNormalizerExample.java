package org.wikapidia.cookbook.sr;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.dataset.DatasetDao;

import java.io.IOException;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class TrainNormalizerExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikapidiaException, InterruptedException {

        Configurator c = new Configurator(new Configuration());
        MonolingualSRMetric sr = c.get(MonolingualSRMetric.class, "default", "language", "simple");
        UniversalSRMetric usr = c.get(UniversalSRMetric.class);

        String path = "../dat/";
        Language l = Language.getByLangCode("simple");

        // This needs to happen at least once...
//        sr.writeCosimilarity("../dat/sr", new LanguageSet(l), 500);

        DatasetDao datasetDao = new DatasetDao();
        Dataset dataset = datasetDao.get(l, "atlasify240.txt");

        sr.trainSimilarity(dataset);
        usr.trainSimilarity(dataset);

        sr.write(path + "/sr/");
        usr.write(path + "/sr/");
    }

}
