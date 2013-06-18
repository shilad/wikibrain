package org.wikapidia.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.PageFilter;
import org.wikapidia.core.dao.SqlDaoIterable;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;
import org.wikapidia.mapper.utils.MapperIterator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class MonolingualConceptMapper extends ConceptMapper {

    private static final AtomicInteger nextUnivId = new AtomicInteger(0);

    public MonolingualConceptMapper(Configurator configurator) {
        super(configurator);
    }

    @Override
    public MapperIterator<UniversalPage> getConceptMap(LanguageSet ls) throws DaoException, ConfigurationException {
        LocalPageDao<LocalPage> dao = configurator.get(LocalPageDao.class);
        SqlDaoIterable<LocalPage> localPages = dao.get(new PageFilter().setLanguages(ls));
        return new MapperIterator<UniversalPage>(localPages) {

            @Override
            public UniversalPage transform(Object obj) {
                LocalPage page = (LocalPage) obj;
                Multimap<Language, LocalPage> map = HashMultimap.create();
                map.put(page.getLanguage(), page);
                return new UniversalPage<LocalPage>(
                        nextUnivId.getAndIncrement(),
                        MONOLINGUAL_ALGORITHM_ID,
                        page.getNameSpace(),
                        map
                );
            }
        };
    }

    public static class Provider extends org.wikapidia.conf.Provider<ConceptMapper> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return ConceptMapper.class;
        }

        @Override
        public String getPath() {
            return "mapper";
        }

        @Override
        public ConceptMapper get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("monolingual")) {
                return null;
            }
            return new MonolingualConceptMapper(getConfigurator());
        }
    }
}
