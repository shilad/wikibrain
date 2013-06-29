package org.wikapidia.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;
import org.wikapidia.mapper.MapperIterator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Ari Weiland
 *
 * A basic ConceptMapper implementation with a very simple algorithm
 * that maps each Local Page across languages to a unique Universal Page.
 *
 */
public class MonolingualConceptMapper extends ConceptMapper {

    private static final AtomicInteger nextUnivId = new AtomicInteger(0);

    public MonolingualConceptMapper(int id, LocalPageDao<LocalPage> localPageDao) {
        super(id, localPageDao);
    }

    @Override
    public MapperIterator<UniversalPage> getConceptMap(LanguageSet ls) throws WikapidiaException {
        Iterable<LocalPage> localPages;
        try {
            localPages = localPageDao.get(new DaoFilter().setLanguages(ls).setRedirect(false));
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }

        if (localPages == null) {
            System.out.println("No pages found!");
            return null;
        }

        return new MapperIterator<UniversalPage>(localPages) {

            @Override
            public UniversalPage transform(Object obj) {
                LocalPage page = (LocalPage) obj;
                Multimap<Language, LocalPage> map = HashMultimap.create();
                map.put(page.getLanguage(), page);
                return new UniversalPage<LocalPage>(
                        nextUnivId.getAndIncrement(),
                        getId(),
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
            return new MonolingualConceptMapper(
                    config.getInt("algorithmId"),
                    getConfigurator().get(
                            LocalPageDao.class,
                            config.getString("localPageDao"))
            );
        }
    }
}
