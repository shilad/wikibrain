package org.wikapidia.mapper;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.utils.MapperIterable;

/**
 */
public abstract class ConceptMapper {

    protected final Configurator configurator;

    protected ConceptMapper(Configurator configurator) {
        this.configurator = configurator;
    }

    public abstract MapperIterable<UniversalPage> getConceptMap(LanguageSet ls) throws DaoException, ConfigurationException;

}
