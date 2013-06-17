package org.wikapidia.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoIterable;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.PageFilter;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;
import org.wikapidia.mapper.utils.MapperIterable;

import java.util.Iterator;

/**
 */
public class MonolingualConceptMapper extends ConceptMapper {

    public MonolingualConceptMapper(Configurator configurator) {
        super(configurator);
    }

    @Override
    public MapperIterable<UniversalPage> getConceptMap(LanguageSet ls) throws DaoException, ConfigurationException {
        LocalPageDao<LocalPage> dao = configurator.get(LocalPageDao.class);
        DaoIterable<LocalPage> localPages = dao.get(new PageFilter().setLanguages(ls));
        return new MapperIterable<UniversalPage>(localPages) {
            @Override
            public UniversalPage transform(Iterator iterator) {
                Object temp = iterator.next();
                LocalPage page = (LocalPage) temp;
                Multimap<Language, LocalPage> map = HashMultimap.create();
                map.put(page.getLanguage(), page);
                return new UniversalPage<LocalPage>(
                        nextUnivId.getAndIncrement(),
                        page.getNameSpace(),
                        map
                );
            }
        };
    }
}
