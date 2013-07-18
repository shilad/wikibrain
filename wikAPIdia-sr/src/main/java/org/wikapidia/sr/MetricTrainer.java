package org.wikapidia.sr;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Lesciko
 * @author Ben Hillmann
 */
public class MetricTrainer {

    public static void main(String[] args) throws ConfigurationException, DaoException, IOException, ClassNotFoundException {

        //Needs command line arguments

        Configurator c = new Configurator(new Configuration());

        LocalSRMetric sr = c.get(LocalSRMetric.class);
        UniversalSRMetric usr = c.get(UniversalSRMetric.class);

        List<String> datasetConfig = c.getConf().get().getStringList("sr.dataset.names");

        if (datasetConfig.size()%2 != 0) {
            throw new ConfigurationException("Datasets must be paired with a matching language");
        }

        String datasetPath = c.getConf().get().getString("sr.dataset.path");
        String normalizerPath = c.getConf().get().getString("sr.normalizers.directory");

        List<Dataset> datasets = new ArrayList<Dataset>();
        DatasetDao datasetDao = new DatasetDao();

        for (int i = 0; i < datasetConfig.size();i+=2) {
            String language = datasetConfig.get(i);
            String datasetName = datasetConfig.get(i+1);
            datasets.add(datasetDao.read(Language.getByLangCode(language), datasetPath + datasetName));
        }

        for (Dataset dataset: datasets) {
            usr.trainSimilarity(dataset);
            usr.trainMostSimilar(dataset,100,null);
            sr.trainDefaultSimilarity(dataset);
            sr.trainDefaultMostSimilar(dataset,100,null);
            sr.trainSimilarity(dataset);
            sr.trainMostSimilar(dataset,100,null);
        }

        usr.write(normalizerPath);
        sr.write(normalizerPath);

        usr.read(normalizerPath);
        sr.read(normalizerPath);



        System.out.println(datasets.get(0));



    }
}
