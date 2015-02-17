package org.wikibrain.mapper.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.ConceptMapper;
import org.wikibrain.mapper.MapperIterator;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * A basic ConceptMapper implementation with a very simple algorithm
 * that maps each Local Page across languages to a unique Universal Page.
 *
 * @author Ari Weiland
 *
 */
public class MonolingualConceptMapper extends ConceptMapper {

    private static final AtomicInteger nextUnivId = new AtomicInteger(0);

    public MonolingualConceptMapper(int id, LocalPageDao localPageDao) {
        super(id, localPageDao);
    }

    @Override
    public MapperIterator<UniversalPage> getConceptMap(LanguageSet ls) throws WikiBrainException {
        Iterable<LocalPage> localPages;
        try {
            localPages = localPageDao.get(new DaoFilter().setLanguages(ls).setRedirect(false));
        } catch (DaoException e) {
            throw new WikiBrainException(e);
        }

        if (localPages == null) {
            System.out.println("No pages found!");
            return null;
        }

        return new MapperIterator<UniversalPage>(localPages) {

            @Override
            public UniversalPage transform(Object obj) {
                LocalPage page = (LocalPage) obj;
                Multimap<Language, LocalId> map = HashMultimap.create();
                map.put(page.getLanguage(), page.toLocalId());
                return new UniversalPage(
                        nextUnivId.getAndIncrement(),
                        getId(),
                        page.getNameSpace(),
                        map
                );
            }
        };
    }

    public static class Provider extends org.wikibrain.conf.Provider<ConceptMapper> {
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
        public ConceptMapper get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
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
