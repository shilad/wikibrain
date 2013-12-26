package org.wikapidia.cookbook.sr;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResultList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class CosimilarityMatrixExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, WikapidiaException, InterruptedException, IOException {
        //Set-up
        Configurator c = new Configurator(new Configuration());
        MonolingualSRMetric sr = c.get(MonolingualSRMetric.class, "default", "language", "simple");
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        UniversalPageDao universalPageDao = c.get(UniversalPageDao.class);
        String path = c.getConf().get().getString("sr.metric.path");

        Language language = Language.getByLangCode("simple");
        sr.writeMostSimilarCache(100);
//        UniversalSRMetric usr = c.get(UniversalSRMetric.class);
//        usr.writeMostSimilarCache(path,100);

        List<String> phrases = Arrays.asList("United States", "Barack Obama", "brain", "natural language processing");

        for (String phrase : phrases){
            System.out.println("\nMost similar to "+phrase+":");
            SRResultList results = sr.mostSimilar(phrase, 5);
            for (int i=0; i<results.numDocs(); i++){
                LocalPage page = localPageDao.getById(language,results.get(i).getId());
                String name = page.getTitle().getCanonicalTitle();
                System.out.println("#"+(i+1)+" "+name);
            }
        }

//        for (LocalString phrase : phrases){
//            System.out.println("\nMost similar to "+phrase.getString()+":");
//            SRResultList results = usr.mostSimilar(phrase, 5);
//            for (int i=0; i<results.numDocs(); i++){
//                UniversalPage page = universalPageDao.getById(results.get(i).getId(),usr.getAlgorithmId());
//                LocalId nameId = (LocalId) page.getLocalIds(page.getLanguageSet().getDefaultLanguage()).toArray()[0];
//                LocalPage namePage = localPageDao.getById(nameId.getLanguage(),nameId.getId());
//                String name = namePage.getTitle().getCanonicalTitle();
//                System.out.println("#"+(i+1)+" "+name);
//            }
//        }

    }
}
