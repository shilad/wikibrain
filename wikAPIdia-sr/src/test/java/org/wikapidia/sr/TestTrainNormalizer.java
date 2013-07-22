package org.wikapidia.sr;

import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;

import java.io.File;
import java.io.IOException;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class TestTrainNormalizer {
    @Test
    public void test() throws ConfigurationException, DaoException, IOException, WikapidiaException, InterruptedException {

        Configurator c = new Configurator(new Configuration());
        LocalSRMetric sr = c.get(LocalSRMetric.class);
        UniversalSRMetric usr = c.get(UniversalSRMetric.class);

//        int i = 0;
//        while(i < 30) {
//            SRResult result = sr.similarity("noon", "string", Language.getByLangCode("simple"), false);
//            System.out.println(result.getValue());
//            i++;
//        }
        String path = "../dat/";
        Language l = Language.getByLangCode("simple");
        SparseMatrix mLocal = new SparseMatrix(new File(path + "sr/matrix/MilneWitten-simple-feature"));
        SparseMatrix mUniversal = new SparseMatrix(new File(path + "sr/matrix/MilneWitten-0-feature"));




        sr.setMostSimilarLocalMatrix(l, mLocal);
        usr.setMostSimilarUniversalMatrix(mUniversal);
        DatasetDao datasetDao = new DatasetDao();
        Dataset dataset = datasetDao.read(l, path + "gold/cleaned/MC.txt");

        sr.trainDefaultSimilarity(dataset);
        usr.trainSimilarity(dataset);
    }

}
