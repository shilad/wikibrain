package org.wikapidia.mapper.algorithms;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;
import org.wikapidia.mapper.utils.MapperIterable;

/**
 */
public class MonolingualConceptMapper extends ConceptMapper {

    protected MonolingualConceptMapper(Configurator configurator) {
        super(configurator);
    }

    @Override
    public MapperIterable<UniversalPage> getConceptMap(LanguageSet ls) throws DaoException, ConfigurationException {
        LocalPageDao dao = configurator.get(LocalPageDao.class);

        return null;
    }
}
