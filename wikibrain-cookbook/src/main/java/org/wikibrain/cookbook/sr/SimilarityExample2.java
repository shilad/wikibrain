package org.wikibrain.cookbook.sr;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;

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

            SRMetric sr = conf.get(
                    SRMetric.class, "ensemble",
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
