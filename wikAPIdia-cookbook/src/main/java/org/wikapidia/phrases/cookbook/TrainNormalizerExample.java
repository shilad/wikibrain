package org.wikapidia.phrases.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.UniversalSRMetric;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;

import java.io.File;
import java.io.IOException;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class TrainNormalizerExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikapidiaException, InterruptedException {

        Configurator c = new Configurator(new Configuration());
        LocalSRMetric sr = c.get(LocalSRMetric.class);
        UniversalSRMetric usr = c.get(UniversalSRMetric.class);

        String path = "../dat/";
        Language l = Language.getByLangCode("simple");
        SparseMatrix mLocal = new SparseMatrix(new File(path + "sr/LocalMilneWitten/matrix/simple-feature"));
        SparseMatrix mUniversal = new SparseMatrix(new File(path + "sr/UniversalMilneWitten/matrix/0-feature"));

        sr.setMostSimilarLocalMatrix(l, mLocal);
        usr.setMostSimilarUniversalMatrix(mUniversal);
        DatasetDao datasetDao = new DatasetDao();
        Dataset dataset = datasetDao.read(l, path + "gold/cleaned/atlasify240.txt");

        sr.trainDefaultSimilarity(dataset);
        usr.trainSimilarity(dataset);

        sr.write(path + "/sr/");
        usr.write(path + "/sr/");
    }

}
