package org.wikapidia.cookbook.sr;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;

/**
 * @author Shilad Sen
 */
public class SimilarityExample2 {
    public static void main(String[] args) {

        SRResult s = null;
        try {
            Env env = new EnvBuilder().build();
            Configurator conf = env.getConfigurator();
            LocalPageDao lpDao = conf.get(LocalPageDao.class);

            Language simple = Language.getByLangCode("simple");

            MonolingualSRMetric sr = conf.get(
                    MonolingualSRMetric.class, "ensemble",
                    "language", simple.getLangCode());

            s = sr.similarity("cat","kitty",true);

        } catch (ConfigurationException e) {
            System.out.println("Configuration Exception: "+e.getMessage());
        } catch (DaoException e) {
            System.out.println("Dao Exception: "+e.getMessage());

        }
        System.out.println("The score for this two pages:"+s.getScore());
    }
}
