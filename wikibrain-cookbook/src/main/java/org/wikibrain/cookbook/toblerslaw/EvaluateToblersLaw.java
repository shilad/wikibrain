package org.wikibrain.cookbook.toblerslaw;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.sr.MonolingualSRMetric;

/**
 * @author Shilad Sen
 */
public class EvaluateToblersLaw {
    public static void main(String args[]) throws ConfigurationException {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator cfg = env.getConfigurator();

        UniversalPageDao upDao = cfg.get(UniversalPageDao.class);
        MonolingualSRMetric enSr = cfg.get(MonolingualSRMetric.class, "ensemble", "language", "en");
        MonolingualSRMetric deSr = cfg.get(MonolingualSRMetric.class, "ensemble", "language", "de");
    }
}
