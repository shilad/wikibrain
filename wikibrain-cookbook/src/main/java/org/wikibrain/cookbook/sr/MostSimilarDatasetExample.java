package org.wikibrain.cookbook.sr;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;
import org.wikibrain.sr.evaluation.MostSimilarDataset;
import org.wikibrain.sr.utils.KnownSim;

import java.util.List;

/**
 * @author Shilad Sen
 */
public class MostSimilarDatasetExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = new EnvBuilder().build();
        DatasetDao dao = env.getConfigurator().get(DatasetDao.class);
        List<Dataset> allEn = dao.getAllInLanguage(Language.getByLangCode("simple"));
//        for (int i = 0; i < allEn.size(); i++) {
//            if (allEn.get(i).getName().equals("WikiSimi3000.txt")) {
//                allEn.remove(i);
//                break;
//            }
//        }
        MostSimilarDataset msd = new MostSimilarDataset(allEn);


        int histogram[] = new int[10000];
        int max = 0;

        for (String phrase : msd.getPhrases()) {
            List<KnownSim> sims = msd.getSimilarities(phrase).getMostSimilar();
            histogram[sims.size()]++;
            max = Math.max(max, sims.size());
            if (sims.size() >= 5) {
                System.out.println("phrase " + phrase + ":");
                for (int i = 0; i < sims.size(); i++) {
                    System.out.println("\t" + (i+1) + ". " + sims.get(i));
                }
            }
        }

        System.out.println("histogram of similar list sizes:");
        for (int i = 1; i <= max; i++) {
            if (histogram[i] > 0) {
                System.out.println("\tsize " + i +": " + histogram[i] + " phrases");
            }
        }
    }
}
