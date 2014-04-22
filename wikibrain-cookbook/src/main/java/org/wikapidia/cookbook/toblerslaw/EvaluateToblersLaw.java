package org.wikapidia.cookbook.toblerslaw;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.sr.MonolingualSRMetric;

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
