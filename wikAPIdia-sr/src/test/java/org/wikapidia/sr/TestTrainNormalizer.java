package org.wikapidia.sr;

import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class TestTrainNormalizer {
    @Test
    public void test() throws ConfigurationException, DaoException {

        Configurator c = new Configurator(new Configuration());
        LocalSRMetric sr = c.get(LocalSRMetric.class);

//        int i = 0;
//        while(i < 30) {
//            SRResult result = sr.similarity("noon", "string", Language.getByLangCode("simple"), false);
//            System.out.println(result.getValue());
//            i++;
//        }

        String path = "/Users/research/IdeaProjects/wikapidia/dat/gold/cleaned/MC.txt";
        DatasetDao datasetDao = new DatasetDao();
        Dataset dataset = datasetDao.read(Language.getByLangCode("simple"), path);

        sr.trainDefaultSimilarity(dataset);

    }

}
