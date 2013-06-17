package org.wikapidia.mapper;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalPage;

/**
 */
public abstract class ConceptMapper {

    public static final int MONOLINGUAL_ALGORITHM_ID = 0;
    public static final int CONCEPTUALIGN_ALGORITHM_ID = 1;

    protected final Configurator configurator;

    protected ConceptMapper(Configurator configurator) {
        this.configurator = configurator;
    }

    public abstract Iterable<UniversalPage> getConceptMap(LanguageSet ls) throws DaoException, ConfigurationException;

}
